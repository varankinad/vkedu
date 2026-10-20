package com.example.vkedu.presentation.appslist

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

@Suppress("CheckReturnValue")
val AppsPlusIcon: ImageVector
    get() {
        if (_appsPlusIcon != null) {
            return _appsPlusIcon!!
        }
        _appsPlusIcon =
            ImageVector.Builder(
                name = "AppsPlus",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 24f,
                viewportHeight = 24f,
            )
                .apply {
                    path(
                        fill = SolidColor(Color.Black),
                        fillAlpha = 1f,
                        stroke = null,
                        strokeAlpha = 1f,
                        strokeLineWidth = 1f,
                        strokeLineCap = StrokeCap.Butt,
                        strokeLineJoin = StrokeJoin.Bevel,
                        strokeLineMiter = 1f,
                        pathFillType = PathFillType.NonZero,
                    ) {
                        moveTo(10f, 9f)
                        close()
                        moveTo(7f, 15f)
                        close()
                        moveToRelative(6f, 3f)
                        verticalLineTo(15f)
                        horizontalLineToRelative(7f)
                        verticalLineToRelative(3f)
                        horizontalLineTo(13f)
                        close()
                        moveTo(4f, 9f)
                        verticalLineTo(6f)
                        horizontalLineToRelative(6f)
                        verticalLineTo(9f)
                        horizontalLineTo(4f)
                        close()
                        moveToRelative(0f, 9f)
                        verticalLineTo(15f)
                        horizontalLineTo(7f)
                        verticalLineToRelative(3f)
                        horizontalLineTo(4f)
                        close()
                        moveToRelative(7f, 2f)
                        horizontalLineTo(22f)
                        verticalLineTo(13f)
                        horizontalLineTo(11f)
                        verticalLineToRelative(7f)
                        close()
                        moveTo(2f, 11f)
                        horizontalLineTo(12f)
                        verticalLineTo(4f)
                        horizontalLineTo(2f)
                        verticalLineToRelative(7f)
                        close()
                        moveToRelative(0f, 9f)
                        horizontalLineTo(9f)
                        verticalLineTo(13f)
                        horizontalLineTo(2f)
                        verticalLineToRelative(7f)
                        close()
                        moveTo(13f, 15f)
                        close()
                        moveToRelative(4f, -4f)
                        verticalLineTo(8f)
                        horizontalLineTo(14f)
                        verticalLineTo(6f)
                        horizontalLineToRelative(3f)
                        verticalLineTo(3f)
                        horizontalLineToRelative(2f)
                        verticalLineTo(6f)
                        horizontalLineToRelative(3f)
                        verticalLineTo(8f)
                        horizontalLineTo(19f)
                        verticalLineToRelative(3f)
                        horizontalLineTo(17f)
                        close()
                    }
                }
                .build()
        return _appsPlusIcon!!
    }

private var _appsPlusIcon: ImageVector? = null
