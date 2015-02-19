package com.bme.shawn.wobble;

/**
 * Minimum Polygon class for Android.
 */
public class Polygon
{
    // Polygon coodinates.
    private float[] polyY, polyX;

    // Number of sides in the polygon.
    private int polySides;

    /**
     * Default constructor.
     * @param px Polygon y coods.
     * @param py Polygon x coods.
     * @param ps Polygon sides count.
     */
    public Polygon( float[] px, float[] py, int ps )
    {
        polyX = px;
        polyY = py;
        polySides = ps;
    }

    /**
     * Checks if the Polygon contains a point.
     * @see "http://alienryderflex.com/polygon/"
     * @param x Point horizontal pos.
     * @param y Point vertical pos.
     * @return Point is in Poly flag.
     */
    public boolean contains( float x, float y )
    {
        boolean oddTransitions = false;
        for( int i = 0, j = polySides -1; i < polySides; j = i++ )
        {
            if( ( polyY[ i ] < y && polyY[ j ] >= y ) || ( polyY[ j ] < y && polyY[ i ] >= y ) )
            {
                if( polyX[ i ] + ( y - polyY[ i ] ) / ( polyY[ j ] - polyY[ i ] ) * ( polyX[ j ] - polyX[ i ] ) < x )
                {
                    oddTransitions = !oddTransitions;
                }
            }
        }
        return oddTransitions;
    }
}