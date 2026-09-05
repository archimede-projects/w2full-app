package com.archimede.w2full.ui.history

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.awaitEachGesture
import androidx.compose.ui.input.pointer.awaitFirstDown
import androidx.compose.ui.input.pointer.awaitPointerEvent
import androidx.compose.ui.input.pointer.consume
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.archimede.w2full.data.repository.PriceHistoryPoint
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

private val AxisDayFormatter = DateTimeFormatter.ofPattern("d MMM", Locale.ITALY)
private val SelectionDayFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.ITALY)

@Composable
internal fun InteractivePriceHistoryChart(
    seriesA: List<PriceHistoryPoint>,
    seriesB: List<PriceHistoryPoint>,
    labelA: String,
    labelB: String,
    showSeriesB: Boolean,
    resetKey: Any?,
    modifier: Modifier = Modifier,
) {
    val effectiveSeriesB = if (showSeriesB) seriesB else emptyList()
    val bounds = remember(seriesA, effectiveSeriesB) { historyChartBounds(seriesA, effectiveSeriesB) }
    if (bounds == null) return

    var viewport by remember(bounds) { mutableStateOf(defaultHistoryChartViewport(bounds)) }
    var selection by remember(bounds) { mutableStateOf<HistoryChartSelection?>(null) }

    LaunchedEffect(resetKey, bounds) {
        viewport = defaultHistoryChartViewport(bounds)
        selection = null
    }

    val density = LocalDensity.current
    val leftPaddingPx = with(density) { 54.dp.toPx() }
    val rightPaddingPx = with(density) { 12.dp.toPx() }

    fun plotWidth(totalWidth: Float): Float = (totalWidth - leftPaddingPx - rightPaddingPx).coerceAtLeast(1f)
    fun xFraction(x: Float, totalWidth: Float): Float =
        ((x - leftPaddingPx) / plotWidth(totalWidth)).coerceIn(0f, 1f)

    val gestureModifier = Modifier
        .fillMaxSize()
        .pointerInput(bounds, seriesA, effectiveSeriesB) {
            detectTapGestures(
                onTap = { offset ->
                    selection = historyChartSelection(
                        seriesA = seriesA,
                        seriesB = effectiveSeriesB,
                        viewport = viewport,
                        xFraction = xFraction(offset.x, size.width.toFloat()),
                    )
                },
                onDoubleTap = {
                    viewport = defaultHistoryChartViewport(bounds)
                    selection = null
                },
            )
        }
        .pointerInput(bounds, seriesA, effectiveSeriesB) {
            detectHorizontalDragGestures(
                onDragStart = { offset ->
                    if (!isHistoryViewportZoomed(viewport, bounds)) {
                        selection = historyChartSelection(
                            seriesA,
                            effectiveSeriesB,
                            viewport,
                            xFraction(offset.x, size.width.toFloat()),
                        )
                    }
                },
                onHorizontalDrag = { change, dragAmount ->
                    if (isHistoryViewportZoomed(viewport, bounds)) {
                        viewport = panHistoryViewport(
                            viewport = viewport,
                            bounds = bounds,
                            dragFraction = dragAmount / plotWidth(size.width.toFloat()),
                        )
                    } else {
                        selection = historyChartSelection(
                            seriesA,
                            effectiveSeriesB,
                            viewport,
                            xFraction(change.position.x, size.width.toFloat()),
                        )
                    }
                    change.consume()
                },
            )
        }
        .pointerInput(bounds) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false)
                var previousDistance: Float? = null
                while (true) {
                    val event = awaitPointerEvent()
                    val pressed = event.changes.filter { it.pressed }
                    if (pressed.isEmpty()) break
                    if (pressed.size >= 2) {
                        val first = pressed[0].position
                        val second = pressed[1].position
                        val distance = (first - second).getDistance()
                        val center = Offset((first.x + second.x) / 2f, (first.y + second.y) / 2f)
                        previousDistance?.let { previous ->
                            if (previous > 0f && distance > 0f) {
                                val zoomFactor = (distance / previous).coerceIn(0.70f, 1.30f)
                                if (abs(zoomFactor - 1f) > 0.005f) {
                                    viewport = zoomHistoryViewport(
                                        viewport = viewport,
                                        bounds = bounds,
                                        zoomFactor = zoomFactor,
                                        focusFraction = xFraction(center.x, size.width.toFloat()),
                                    )
                                    selection = null
                                }
                            }
                        }
                        previousDistance = distance
                        pressed.forEach { it.consume() }
                    } else {
                        previousDistance = null
                    }
                }
            }
        }

    val axis = remember(seriesA, effectiveSeriesB, viewport) {
        historyPriceAxisScale(seriesA, effectiveSeriesB, viewport)
    }
    val renderA = remember(seriesA, viewport, axis) {
        axis?.let { historyRenderPoints(seriesA, viewport, it) }.orEmpty()
    }
    val renderB = remember(effectiveSeriesB, viewport, axis) {
        axis?.let { historyRenderPoints(effectiveSeriesB, viewport, it) }.orEmpty()
    }
    val dateTicks = remember(viewport) { historyDateTicks(viewport, tickCount = 3) }

    val colorA = MaterialTheme.colorScheme.primary
    val colorB = MaterialTheme.colorScheme.tertiary
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val axisTextColor = MaterialTheme.colorScheme.onSurfaceVariant
    val selectionColor = MaterialTheme.colorScheme.onSurface

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Canvas(modifier = gestureModifier.weight(1f)) {
            val left = 54.dp.toPx()
            val right = 12.dp.toPx()
            val top = 14.dp.toPx()
            val bottom = 30.dp.toPx()
            val width = (size.width - left - right).coerceAtLeast(1f)
            val height = (size.height - top - bottom).coerceAtLeast(1f)
            val currentAxis = axis ?: return@Canvas
            val priceSpan = (currentAxis.maxPriceMilliEuro - currentAxis.minPriceMilliEuro).coerceAtLeast(1L).toDouble()

            val yPaint = Paint().apply {
                color = axisTextColor.toArgb()
                textSize = 10.sp.toPx()
                isAntiAlias = true
                textAlign = Paint.Align.RIGHT
            }
            val xPaint = Paint().apply {
                color = axisTextColor.toArgb()
                textSize = 10.sp.toPx()
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
            }
            val unitPaint = Paint().apply {
                color = axisTextColor.toArgb()
                textSize = 10.sp.toPx()
                isAntiAlias = true
                textAlign = Paint.Align.LEFT
            }

            currentAxis.ticksMilliEuro.forEach { tick ->
                val fraction = (tick - currentAxis.minPriceMilliEuro) / priceSpan
                val y = top + height * (1.0 - fraction).toFloat()
                drawLine(
                    color = gridColor,
                    start = Offset(left, y),
                    end = Offset(left + width, y),
                    strokeWidth = 1.dp.toPx(),
                )
                drawIntoCanvas { canvas ->
                    canvas.nativeCanvas.drawText(formatAxisPrice(tick), left - 6.dp.toPx(), y + 3.dp.toPx(), yPaint)
                }
            }
            drawIntoCanvas { canvas ->
                canvas.nativeCanvas.drawText("€/L", 2.dp.toPx(), top - 2.dp.toPx(), unitPaint)
            }

            dateTicks.forEach { epochDay ->
                val fraction = ((epochDay - viewport.startEpochDay) / viewport.spanDays).toFloat().coerceIn(0f, 1f)
                val x = left + fraction * width
                drawIntoCanvas { canvas ->
                    canvas.nativeCanvas.drawText(
                        formatAxisDate(epochDay),
                        x,
                        size.height - 7.dp.toPx(),
                        xPaint,
                    )
                }
            }

            fun drawSeries(points: List<HistoryChartRenderPoint>, color: Color) {
                val canvasPoints = points.map { point ->
                    point to Offset(
                        x = left + point.xFraction * width,
                        y = top + point.yFraction * height,
                    )
                }
                canvasPoints.zipWithNext().forEach { (start, end) ->
                    if (end.first.epochDay - start.first.epochDay <= 1L) {
                        drawLine(
                            color = color,
                            start = start.second,
                            end = end.second,
                            strokeWidth = 3.dp.toPx(),
                            cap = StrokeCap.Round,
                        )
                    }
                }
                canvasPoints.forEach { (_, point) ->
                    drawCircle(color = color, radius = 4.5.dp.toPx(), center = point)
                }
            }

            drawSeries(renderA, colorA)
            drawSeries(renderB, colorB)

            selection?.let { selected ->
                val selectedDay = selected.epochDay.toDouble()
                if (selectedDay >= viewport.startEpochDay && selectedDay <= viewport.endEpochDay) {
                    val x = left + ((selectedDay - viewport.startEpochDay) / viewport.spanDays).toFloat() * width
                    drawLine(
                        color = selectionColor,
                        start = Offset(x, top),
                        end = Offset(x, top + height),
                        strokeWidth = 1.5.dp.toPx(),
                    )
                }
            }
        }

        selection?.let { selected ->
            Text(
                text = selectionLabel(selected, labelA, labelB, showSeriesB),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth(),
                maxLines = 2,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("● $labelA", color = colorA, style = MaterialTheme.typography.bodySmall)
            if (showSeriesB) {
                Text("● $labelB", color = colorB, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

private fun formatAxisPrice(priceMilliEuro: Long): String =
    String.format(Locale.ITALY, "%.3f", priceMilliEuro / 1_000.0)

private fun formatAxisDate(epochDay: Long): String =
    LocalDate.ofEpochDay(epochDay).format(AxisDayFormatter).replace(".", "")

private fun selectionLabel(
    selection: HistoryChartSelection,
    labelA: String,
    labelB: String,
    showSeriesB: Boolean,
): String {
    val date = LocalDate.ofEpochDay(selection.epochDay).format(SelectionDayFormatter)
    val a = selection.seriesAPriceMilliEuro?.let(::formatSelectionPrice) ?: "n.d."
    val parts = mutableListOf("$date · $labelA $a")
    if (showSeriesB) {
        val b = selection.seriesBPriceMilliEuro?.let(::formatSelectionPrice) ?: "n.d."
        parts += "$labelB $b"
    }
    return parts.joinToString(" · ")
}

private fun formatSelectionPrice(priceMilliEuro: Long): String =
    String.format(Locale.ITALY, "€ %.3f/L", priceMilliEuro / 1_000.0)
