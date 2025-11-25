package com.example.individualassignment6_q4

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.individualassignment6_q4.ui.theme.IndividualAssignment6_q4Theme
import kotlin.math.abs

/**
 * Main Activity for the Tilt Maze Game
 * Sets up the app theme and displays the game
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            IndividualAssignment6_q4Theme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    TiltMazeGame(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

/**
 * Data class representing the ball in the game
 * @param x X-coordinate position
 * @param y Y-coordinate position
 * @param radius Size of the ball
 */
data class Ball(var x: Float, var y: Float, val radius: Float = 30f)

/**
 * Data class representing a wall obstacle
 * @param rect The rectangular boundary of the wall
 */
data class Wall(val rect: Rect)

/**
 * Main game composable that handles the tilt maze game logic and UI
 * Uses accelerometer sensor to control ball movement
 */
@Composable
fun TiltMazeGame(modifier: Modifier = Modifier) {
    // Get the sensor manager to access device sensors
    val context = LocalContext.current
    val sensorManager = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    val accelerometer = remember { sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) }

    // Ball starting position in top left area
    var ball by remember { mutableStateOf(Ball(80f, 80f)) }
    // Physics variables for ball movement
    var velocityX by remember { mutableFloatStateOf(0f) }
    var velocityY by remember { mutableFloatStateOf(0f) }
    // Track if player has won the game
    var gameWon by remember { mutableStateOf(false) }

    // Define all walls in the maze - simple zigzag path to the goal
    val walls = remember {
        listOf(
            // OUTER BOUNDARY WALLS
            Wall(Rect(0f, 0f, 15f, 1000f)),           // Left boundary
            Wall(Rect(0f, 0f, 1000f, 15f)),           // Top boundary
            Wall(Rect(985f, 0f, 1000f, 1000f)),       // Right boundary
            Wall(Rect(0f, 985f, 1000f, 1000f)),       // Bottom boundary

            //  SIMPLE MAZE OBSTACLES
            // Creates a zigzag path from top-left to bottom-right
            Wall(Rect(200f, 200f, 220f, 600f)),       // Vertical barrier 1
            Wall(Rect(220f, 580f, 600f, 600f)),       // Horizontal barrier 1

            Wall(Rect(580f, 400f, 600f, 600f)),       // Vertical barrier 2
            Wall(Rect(400f, 400f, 600f, 420f)),       // Horizontal barrier 2

            Wall(Rect(400f, 200f, 420f, 420f)),       // Vertical barrier 3
            Wall(Rect(420f, 200f, 800f, 220f)),       // Horizontal barrier 3

            Wall(Rect(780f, 220f, 800f, 800f)),       // Vertical barrier 4 (final corridor)
        )
    }

    // GOAL AREA
    // Define the goal zone in the bottom right corner
    val goal = remember { Rect(820f, 820f, 960f, 960f) }

    // ACCELEROMETER SENSOR LISTENER
    // This effect sets up the sensor listener and cleans it up when done
    DisposableEffect(Unit) {
        val listener = object : SensorEventListener {
            /**
             * Called when sensor values change
             * This is where we read accelerometer data and update ball position
             */
            override fun onSensorChanged(event: SensorEvent?) {
                event?.let {
                    // Only process movement if game hasn't been won yet
                    if (!gameWon) {
                        //  READ TILT VALUES
                        // event.values[0] = tilt left/right (X-axis)
                        // event.values[1] = tilt forward/backward (Y-axis)
                        // Negative sign inverts X for natural tilt direction
                        val tiltX = -event.values[0]
                        val tiltY = event.values[1]

                        //  APPLY PHYSICS
                        // Convert tilt to acceleration (multiplier controls sensitivity)
                        velocityX += tiltX * 0.4f
                        velocityY += tiltY * 0.4f

                        // Apply friction to slow down ball naturally
                        // 0.98 means ball retains 98% of velocity each frame
                        velocityX *= 0.98f
                        velocityY *= 0.98f

                        // Limit maximum speed to prevent ball from moving too fast
                        val maxSpeed = 12f
                        velocityX = velocityX.coerceIn(-maxSpeed, maxSpeed)
                        velocityY = velocityY.coerceIn(-maxSpeed, maxSpeed)

                        // CALCULATE NEW POSITION
                        // Add velocity to current position
                        var newX = ball.x + velocityX
                        var newY = ball.y + velocityY

                        // COLLISION DETECTION
                        // Check each wall for collision with the ball
                        var collision = false
                        for (wall in walls) {
                            // Check if ball collides with this wall
                            if (checkCollision(newX, newY, ball.radius, wall.rect)) {
                                collision = true

                                // BOUNCE PHYSICS
                                // Check collision on X-axis (horizontal movement)
                                if (checkCollision(newX, ball.y, ball.radius, wall.rect)) {
                                    // Reverse and dampen horizontal velocity
                                    velocityX = -velocityX * 0.5f
                                    newX = ball.x  // Keep old X position
                                }
                                // Check collision on Y axis (vertical movement)
                                if (checkCollision(ball.x, newY, ball.radius, wall.rect)) {
                                    // Reverse and dampen vertical velocity
                                    velocityY = -velocityY * 0.5f
                                    newY = ball.y  // Keep old Y position
                                }
                            }
                        }

                        // UPDATE BALL POSITION
                        // Apply the new position (after collision checks)

                        ball = ball.copy(x = newX, y = newY)

                        // CHECK WIN
                        // See if ball has reached the goal area
                        if (checkCollision(ball.x, ball.y, ball.radius, goal)) {
                            gameWon = true
                        }
                    }
                }
            }

            /**
             * Called when sensor accuracy changes
             * Not used in this game, but required by interface
             */
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        // Register the sensor listener with GAME delay for good performance
        sensorManager.registerListener(
            listener,
            accelerometer,
            SensorManager.SENSOR_DELAY_GAME  // ~20ms between updates
        )

        // unregister sensor when composable leaves composition
        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }

    // Main column containing all game UI elements
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // GAME TITLE
        // Displays game title or win message with card background
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (gameWon) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Text(
                text = if (gameWon) "🎉 VICTORY! 🎉" else "Tilt Maze Challenge",
                style = MaterialTheme.typography.headlineMedium,
                color = if (gameWon) Color.White else MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier
                    .padding(16.dp)
                    .align(Alignment.CenterHorizontally)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // game canvas
        // Main drawing area for the maze game
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)  // Keep it square
                .padding(horizontal = 16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                // Calculate scale factor to fit 1000x1000 coordinate system to canvas
                val scale = size.width / 1000f

                // Light gradient-like background
                drawRect(Color(0xFFECEFF1))

                // Green zone where player needs to reach
                drawRect(
                    color = if (gameWon) Color(0xFFFFD700) else Color(0xFF66BB6A),  // Gold when won
                    topLeft = Offset(goal.left * scale, goal.top * scale),
                    size = Size(goal.width * scale, goal.height * scale)
                )

                // Draw target circles in goal area for visual indicator
                drawCircle(
                    color = Color.White.copy(alpha = 0.3f),
                    radius = 50f * scale,
                    center = Offset(
                        (goal.left + goal.width / 2) * scale,
                        (goal.top + goal.height / 2) * scale
                    )
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.5f),
                    radius = 30f * scale,
                    center = Offset(
                        (goal.left + goal.width / 2) * scale,
                        (goal.top + goal.height / 2) * scale
                    )
                )

                // Draw all maze walls with shadow effect
                walls.forEach { wall ->
                    // Shadow layer for depth
                    drawRect(
                        color = Color.Black.copy(alpha = 0.3f),
                        topLeft = Offset(
                            wall.rect.left * scale + 3f,
                            wall.rect.top * scale + 3f
                        ),
                        size = Size(wall.rect.width * scale, wall.rect.height * scale)
                    )

                    // Main wall
                    drawRect(
                        color = Color(0xFF37474F),  // Dark blue-gray
                        topLeft = Offset(wall.rect.left * scale, wall.rect.top * scale),
                        size = Size(wall.rect.width * scale, wall.rect.height * scale)
                    )
                }

                // Shadow underneath ball for 3D effect
                drawCircle(
                    color = Color.Black.copy(alpha = 0.25f),
                    radius = ball.radius * scale * 0.9f,
                    center = Offset(
                        ball.x * scale + 4f,
                        ball.y * scale + 4f
                    )
                )

                // Main game ball with gradient-like effect
                // Outer glow
                drawCircle(
                    color = if (gameWon)
                        Color(0xFFFFEB3B).copy(alpha = 0.5f)
                    else
                        Color(0xFF2196F3).copy(alpha = 0.5f),
                    radius = ball.radius * scale * 1.15f,
                    center = Offset(ball.x * scale, ball.y * scale)
                )

                // Main ball body
                drawCircle(
                    color = if (gameWon) Color(0xFFFFD700) else Color(0xFF2196F3),  // Blue normally, gold when won
                    radius = ball.radius * scale,
                    center = Offset(ball.x * scale, ball.y * scale)
                )

                // Highlight for 3D sphere effect
                drawCircle(
                    color = Color.White.copy(alpha = 0.4f),
                    radius = ball.radius * scale * 0.5f,
                    center = Offset(
                        ball.x * scale - ball.radius * scale * 0.3f,
                        ball.y * scale - ball.radius * scale * 0.3f
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Show helpful instructions when game is active
        if (!gameWon) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Tilt your phone to move the ball",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = " Navigate through the maze to reach the green goal!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        } else {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF4CAF50)
                )
            ) {
                Text(
                    text = " Congratulations!",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White,
                    modifier = Modifier
                        .padding(16.dp)
                        .align(Alignment.CenterHorizontally)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Button to restart the game
        Button(
            onClick = {
                // Reset all game state
                ball = Ball(80f, 80f)  // Starting position
                velocityX = 0f
                velocityY = 0f
                gameWon = false
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text(
                text = if (gameWon) "Play Again" else "Reset Game",
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

/**
 * Checks if a circle (ball) collides with a rectangle (wall)
 * Uses the closest point algorithm for accurate collision detection
 *
 * @param x X-coordinate of the circle center
 * @param y Y-coordinate of the circle center
 * @param radius Radius of the circle
 * @param rect Rectangle to check collision against
 * @return true if collision detected, false otherwise
 */
fun checkCollision(x: Float, y: Float, radius: Float, rect: Rect): Boolean {
    // This algorithm finds the point on the rectangle that is closest to the circle center
    // If circle center is inside rect, closest point is the center itself
    // If outside, it's clamped to the rectangle's edges
    val closestX = x.coerceIn(rect.left, rect.right)
    val closestY = y.coerceIn(rect.top, rect.bottom)

    // Calculate the distance between the circle center and the closest point
    val distanceX = x - closestX
    val distanceY = y - closestY

    // Use distance squared to avoid expensive sqrt() calculation
    val distanceSquared = distanceX * distanceX + distanceY * distanceY

    // If distance is less than radius, the circle is touching or overlapping the rectangle
    return distanceSquared < (radius * radius)
}