package com.shilapi.xcertplay.orchestration

import com.shilapi.xcertplay.airplay.VideoInCar

/**
 * iOS 27 video in car, played by the host in the car's own player (see [VideoInCar]).
 *
 * This fork has no gear source on the PSA head unit, so playback is never gated on P
 * ([VideoInCar.allowed] is always true). A parked-only gate would live here again.
 */
interface CarPlayVideoListener {
    /** A playback message from the iPhone, e.g. insertPlayQueueItem, setRate, seek, stop. Any thread. */
    fun onVideoMessage(streamId: Long, message: Map<String, Any?>)

    /** The iPhone asks the car to show its video player (requestUI "videoplayback:"). Any thread. */
    fun onVideoUiRequested()

    /** The CarPlay session ended; the player must close. Any thread. */
    fun onVideoSessionEnded()
}
