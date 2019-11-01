package edu.sinica.iis.util

import edu.sinica.iis.util.Interpolation.Entry
import org.scalatest.FlatSpec

class InterpolationSpec extends FlatSpec {
  def isApproximate(num1: Double, num2: Double) = (num1 - num2) < 0.001

  "The distance function" should "calculate the normalized distance" in {
    val testPoint1Lat = 1
    val testPoint1Lon = 1
    val entry = Entry("DEVICE_ID", 1, 1, 1, 1, 1, 1, testPoint1Lat, testPoint1Lon)
    val testPoint2Lat = 2
    val testPoint2Lon = 2
    val normalizedDistance = Interpolation.distance(entry, testPoint2Lat, testPoint2Lon)

    assert(isApproximate(normalizedDistance, 1.414))
  }

  "The interpolate function" should "calculate the interpolated value" in {
    val value = Interpolation.interpolate(25.055222, 121.617254, 343.35992800350016)
    assert(isApproximate(value, 6.699))
  }

  "The interpolate function" should "always be greater than zero" in {
    val value = Interpolation.interpolate(25.065012650154813, 121.58466942425078, 21589.456811731743)
    assert(value >= 0)
  }
}
