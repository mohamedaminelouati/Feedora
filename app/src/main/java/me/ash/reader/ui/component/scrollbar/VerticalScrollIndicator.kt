package me.ash.reader.ui.component.scrollbar

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ScrollIndicatorState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class VerticalScrollIndicatorFactory(
    val thumbThickness: Dp = 4.dp,
    val padding: Dp = 2.dp,
    val thumbColor: Color = Color.Gray,
    val thumbAlpha: Float = 0.5f,
) : ScrollIndicatorFactory {
    // The node is the core of the ScrollIndicator, handling the drawing logic.
    override fun createNode(
        state: ScrollIndicatorState,
        orientation: Orientation,
    ): DelegatableNode {
        return object : Modifier.Node(), DrawModifierNode {

            private val alpha = Animatable(0f)

            override fun onAttach() {
                coroutineScope.launch {
                    snapshotFlow { state.scrollOffset }
                        .collectLatest {
                            alpha.snapTo(1f)
                            delay(3000)
                            alpha.animateTo(0f, animationSpec = spring())
                        }
                }
            }

            override fun ContentDrawScope.draw() {
                // Draw the original content.
                drawContent()

                // Don't draw the scrollbar if the content fits within the viewport.
                if (state.contentSize <= state.viewportSize) return

                if (alpha.value == 0f) return

                val visibleContentRatio = state.viewportSize.toFloat() / state.contentSize

                // Calculate the thumb's size and position along the scrolling axis.
                val thumbLength = state.viewportSize * visibleContentRatio
                val thumbPosition = state.scrollOffset * visibleContentRatio

                val thumbThicknessPx = thumbThickness.toPx()
                val paddingPx = padding.toPx()

                // Determine the scrollbar size and thumb position based on the orientation.
                val (topLeft, size) =
                    when (orientation) {
                        Orientation.Vertical -> {
                            val x = size.width - thumbThicknessPx - paddingPx
                            Offset(x, thumbPosition) to Size(thumbThicknessPx, thumbLength)
                        }
                        Orientation.Horizontal -> {
                            val y = size.height - thumbThicknessPx - paddingPx
                            Offset(thumbPosition, y) to Size(thumbLength, thumbThicknessPx)
                        }
                    }

                // Draw the scrollbar thumb.
                drawRoundRect(
                    cornerRadius = CornerRadius(x = thumbThicknessPx / 2),
                    color = thumbColor,
                    topLeft = topLeft,
                    size = size,
                    alpha = thumbAlpha * alpha.value,
                )
            }
        }
    }
}
