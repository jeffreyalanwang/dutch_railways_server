package com.jeffreyalanwang.util.geolatte.crs

import org.geolatte.geom.crs.CoordinateReferenceSystems as GeoLatteCoordinateReferenceSystems
import org.geolatte.geom.crs.CrsRegistry as GeoLatteCrsRegistry
import org.geolatte.geom.crs.ProjectedCoordinateReferenceSystem as GeoLatteCoordinateReferenceSystem
import org.locationtech.proj4j.CoordinateReferenceSystem as Proj4jCoordinateReferenceSystem
import org.locationtech.proj4j.CRSFactory as Proj4jCrsFactory

/**
 * Manages both GeoLatte and proj4j coordinate reference systems.
 */
internal object DualCrsFactory {
    private val proj4j = Proj4jCrsFactory()

    fun getGeoLatteFromEPSG(epsgCode: Int): GeoLatteCoordinateReferenceSystem =
        GeoLatteCrsRegistry.getProjectedCoordinateReferenceSystemForEPSG(epsgCode)

    fun getProj4jFromEPSG(epsgCode: Int): Proj4jCoordinateReferenceSystem =
        proj4j.createFromName("epsg:$epsgCode")

    /**
     * Get both the GeoLatte and the proj4j coordinate reference systems.
     */
    fun getCrsPairFromEPSG(epsgCode: Int) = Pair(
        getGeoLatteFromEPSG(epsgCode),
        getProj4jFromEPSG(epsgCode),
    )

    val WGS84 = GeoLatteCoordinateReferenceSystems.WGS84 to getProj4jFromEPSG(4326)
}