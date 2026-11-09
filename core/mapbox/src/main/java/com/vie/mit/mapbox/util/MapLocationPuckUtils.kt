package com.vie.mit.mapbox.util

import com.mapbox.maps.ImageHolder
import com.mapbox.maps.extension.style.expressions.dsl.generated.interpolate
import com.mapbox.maps.plugin.LocationPuck2D
import com.mapbox.maps.plugin.locationcomponent.R

/**
 * Creates a standard 2D [LocationPuck2D] with a zoom-interpolated scale expression.
 * The puck scales down when zoomed out and scales up to full size (1.0) when zoomed in.
 *
 * @param withBearing Whether to show an arrow bearing image.
 */
fun createZoomScaled2DPuck(
    withBearing: Boolean = true,
): LocationPuck2D {
    return LocationPuck2D(
        topImage = ImageHolder.from(R.drawable.mapbox_user_icon),
        bearingImage = if (withBearing) {
            ImageHolder.from(R.drawable.mapbox_user_bearing_icon)
        } else {
            ImageHolder.from(R.drawable.mapbox_user_stroke_icon)
        },
        shadowImage = if (withBearing) {
            ImageHolder.from(R.drawable.mapbox_user_stroke_icon)
        } else {
            ImageHolder.from(R.drawable.mapbox_user_icon_shadow)
        },
        scaleExpression = interpolate {
            linear()
            zoom()

            stop {
                literal(10.0)
                literal(0.5)
            }
            stop {
                literal(12.0)
                literal(0.6)
            }
            stop {
                literal(15.0)
                literal(0.8)
            }
            stop {
                literal(20.0)
                literal(1.0)
            }
        }.toJson()
    )
}
