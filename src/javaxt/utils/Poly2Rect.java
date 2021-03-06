package javaxt.utils;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.geom.Area;
import java.awt.geom.PathIterator;
import java.util.*;


//******************************************************************************
//**  Poly2Rect
//******************************************************************************
/**
 *   Used to approximate a polygon with rectangles - a process known as
 *   rectangular decomposition. This algorithm starts by computing the largest
 *   inscribed rectangle within a convex hull. Rectangles are generated around
 *   the inscribed rectangle which provides the general shape of the polygon.
 *   For best results, this function can be called iteratively by clipping the
 *   source polygon with rectangles generated by the getOverlappingRectangles()
 *   method and feeding the clipped polygons into new instances of this class.
 *
 *   Credits:
 *   Daniel Sud for the Inscribed Rectangle algorithm
 *   http://cgm.cs.mcgill.ca/~athens/cs507/Projects/2003/DanielSud/
 *
 *   Project Nayuki for the Convex Hull implementation
 *   https://www.nayuki.io/page/convex-hull-algorithm
 *
 ******************************************************************************/

public class Poly2Rect {

    private Polygon polygon;
    private Polygon hull; //convex hull
    private ArrayList<Point> points; //hull points
    private Rectangle r;
    private ArrayList<Rectangle> rectangles;
    private double minAspectRatio = 0.2;
    private int start, stop; //tangents for iterative convex hull
    private int xmin,xmax,ymin,ymax; //position of hull


  //**************************************************************************
  //** Constructor
  //**************************************************************************
    public Poly2Rect(Polygon polygon) {
        this.polygon = polygon;


      //Compute convex hull
        hull = new ConvexHull(polygon).getHull();
        points = new ArrayList<>();
        for (int i=0; i<hull.npoints; i++){
            int x = hull.xpoints[i];
            int y = hull.ypoints[i];
            addPoint(new Point(x,y));
        }
    }


  //**************************************************************************
  //** getPolygon
  //**************************************************************************
  /** Returns the original polygon used to instantiate this class
   */
    public Polygon getPolygon(){
        return polygon;
    }


  //**************************************************************************
  //** getConvexHull
  //**************************************************************************
  /** Returns the convex hull of the polygon
   */
    public Polygon getConvexHull(){
        return hull;
    }


  //**************************************************************************
  //** getInscribedRectangle
  //**************************************************************************
  /** Returns the largest rectangle that will fit inside a convex hull
   */
    public Rectangle getInscribedRectangle() {
        if (r==null){
            r = computeLargestRectangle();
        }
        return r;
    }


  //**************************************************************************
  //** getOverlappingRectangles
  //**************************************************************************
  /** Returns an array of rectangles used to approximate the polygon
   */
    public ArrayList<Rectangle> getOverlappingRectangles(){
        if (rectangles==null){
            Rectangle r = getInscribedRectangle();
            Rectangle e = polygon.getBounds();
            rectangles = new ArrayList<>();


            ArrayList<Rectangle> arr = new ArrayList<>();

            int widthLeft = r.x-e.x;
            int widthCenter = r.width;
            int widthRight = e.width-(widthLeft+widthCenter);

            int heightTop = r.y-e.y;
            int heightMiddle = r.height;
            int heightBottom = e.height-(heightTop+heightMiddle);


            arr.add(new Rectangle(e.x, e.y, widthLeft, heightTop));
            arr.add(new Rectangle(r.x, e.y, r.width, heightTop));
            arr.add(new Rectangle(r.x+r.width, e.y, widthRight, heightTop));

            arr.add(new Rectangle(e.x, r.y, widthLeft, heightMiddle));
            arr.add(r);
            arr.add(new Rectangle(r.x+r.width, r.y, widthRight, heightMiddle));

            arr.add(new Rectangle(e.x, r.y+r.height, widthLeft, heightBottom));
            arr.add(new Rectangle(r.x, r.y+r.height, widthCenter, heightBottom));
            arr.add(new Rectangle(r.x+r.width, r.y+r.height, widthRight, heightBottom));


            if (getAspectRatio(widthLeft, e.height)<minAspectRatio){
                Rectangle rect;

                arr.set(0, null);
                rect = arr.get(1);
                arr.set(1, new Rectangle(rect.x-widthLeft, rect.y, rect.width+widthLeft, rect.height));


                arr.set(3, null);
                rect = arr.get(4);
                arr.set(4, new Rectangle(rect.x-widthLeft, rect.y, rect.width+widthLeft, rect.height));


                arr.set(6, null);
                rect = arr.get(7);
                arr.set(7, new Rectangle(rect.x-widthLeft, rect.y, rect.width+widthLeft, rect.height));
            }

            if (getAspectRatio(widthRight, e.height)<minAspectRatio){
                Rectangle rect;

                rect = arr.get(1);
                rect.setSize(rect.width+widthRight, rect.height);
                arr.set(1, rect);
                arr.set(2, null);

                rect = arr.get(4);
                rect.setSize(rect.width+widthRight, rect.height);
                arr.set(4, rect);
                arr.set(5, null);

                rect = arr.get(7);
                rect.setSize(rect.width+widthRight, rect.height);
                arr.set(7, rect);
                arr.set(8, null);
            }

            if (getAspectRatio(heightTop, e.width)<minAspectRatio){

                arr.set(0, null);
                arr.set(1, null);
                arr.set(2, null);

                for (int i=3; i<6; i++){
                    Rectangle rect = arr.get(i);
                    if (rect!=null){
                        arr.set(i, new Rectangle(rect.x, rect.y-heightTop, rect.width, rect.height+heightTop));
                    }
                }
            }

            if (getAspectRatio(heightBottom, e.width)<minAspectRatio){

                arr.set(6, null);
                arr.set(7, null);
                arr.set(8, null);

                for (int i=3; i<6; i++){
                    Rectangle rect = arr.get(i);
                    if (rect!=null){
                        arr.set(i, new Rectangle(rect.x, rect.y, rect.width, rect.height+heightBottom));
                    }
                }
            }


            Area a = new Area(polygon);
            for (Rectangle rectangle : arr){
                if (rectangle==null) continue;
                Area b = new Area(rectangle);
                b.intersect(a);
                if (!b.isEmpty()) {
                    rectangles.add(b.getBounds());
                }
            }

        }
        return rectangles;
    }


  //**************************************************************************
  //** divideRectangles
  //**************************************************************************
  /** Used to subdivide rectangles generated by the getOverlappingRectangles()
   *  method
   */
    public static ArrayList<Rectangle> divideRectangles(
        ArrayList<Rectangle> rectangles, Polygon orgPolygon, int minArea){
        if (rectangles.size()==1) return rectangles;
        ArrayList<Rectangle> arr = new ArrayList<>();
        for (Rectangle rectangle : rectangles){

            Area a = new Area(orgPolygon);
            Area b = new Area(rectangle);
            b.intersect(a);
            if (!b.isEmpty()) {


              //Create polygon
                PathIterator iterator = b.getPathIterator(null);
                float[] floats = new float[6];
                Polygon polygon = new Polygon();
                while (!iterator.isDone()) {
                    int type = iterator.currentSegment(floats);
                    int x = (int) floats[0];
                    int y = (int) floats[1];
                    if (type != PathIterator.SEG_CLOSE) {
                        polygon.addPoint(x, y);
                    }
                    iterator.next();
                }


              //Compute area of the new polygon
                double area = 0;
                for (int i=0; i<polygon.npoints; i++){
                    area = area +
                    polygon.xpoints[i]*polygon.ypoints[(i+1)%polygon.npoints] -
                    polygon.ypoints[i]*polygon.xpoints[(i+1)%polygon.npoints];
                }
                area = Math.abs(area / 2);


                /*
              //Compare polygon area to the total area of the rectancle
                int rectArea = rectangle.width * rectangle.height;
                double percentOverlap = Math.round((area/(double)rectArea)*100);
                System.out.println(Math.round(area) + " vs " + rectArea + " == " + percentOverlap + "% " +
                Math.round(getAspectRatio(rectangle)*100.0)/100.0);
                */



                boolean divide = (area>minArea);
                if (divide){
                    try{
                        Poly2Rect pd = new Poly2Rect(polygon);
                        for (Rectangle r : pd.getOverlappingRectangles()){
                            arr.add(r);
                        }
                    }
                    catch(Exception ex){
                        arr.add(rectangle);
                    }
                }
                else{
                    arr.add(rectangle);
                }


            }
            else{
                arr.add(rectangle);
            }
        }
        return arr;
    }





  //**************************************************************************
  //** addPoint
  //**************************************************************************
  /** Used to add a point to the polygon (convex hull). Returns true if the
   *  point was successfully added. Returns false if the point was invalid and
   *  was not added (e.g. bowtie)
   */
    private boolean addPoint(Point p){
        if (points.size()<2){
            points.add(p);
            return true;
        }
        else if (points.size()==2){
            Point ha = points.get(0);
            Point hb = points.get(1);
            if (onLeft(ha, hb, p)){
                points.add(p);
                return true;
            }
            else{
                points.add(1, p); //points.insertElementAt(p, 1);
                return true;
            }
        }
        else{
            return addPointToHull(p);
        }
    }


  //**************************************************************************
  //** onLeft
  //**************************************************************************
  /** position of point w.r.t. hull edge
   *  sign of twice the area of triangle abc
   */
    private boolean onLeft(Point a, Point b, Point c){
        int area = (b.x -a.x)*(c.y - a.y) - (c.x - a.x)*(b.y - a.y);
        return (area<0);
    }


  //**************************************************************************
  //** pointOutside
  //**************************************************************************
  /** check if point is outside
   *  true is point is on right of all vertices
   *  finds tangents if point is outside
   */
    private boolean pointOutside(Point p){

        boolean ptIn = true, currIn, prevIn = true;

        Point a = points.get(0);
        Point b;

        for(int i=0; i<points.size(); i++){

            b = points.get((i+1)%points.size());
            currIn = onLeft(a, b, p);
            ptIn = ptIn && currIn;
            a = b;

            if(prevIn && !currIn){ start = i;} /* next point outside, 1st tangent found */
            if(!prevIn && currIn){ stop = i;}  /* 2nd tangent */
            prevIn = currIn;

        }
        return !ptIn;
    }


  //**************************************************************************
  //** addPointToHull
  //**************************************************************************
  /** check if point is outside, insert it, maintaining general position
   */
    private boolean addPointToHull(Point p) {

        /* index of tangents */
        start=0;
        stop=0;

        if(!pointOutside(p)){
            return false;
        }

        /* insert point */
        int numRemove;

        if (stop > start){
            numRemove = stop-start-1;
            if (numRemove>0){
                removeRange(start+1, stop);
            }
            points.add(start+1, p);
        }
        else{
            numRemove = stop+points.size()-start-1;
            if (numRemove > 0){
                if (start+1 < points.size()){
                    removeRange(start+1, points.size());
                }
                if(stop-1 >= 0){
                    removeRange(0, stop);
                }
            }
            points.add(p);

        }
        return true;
    }


  //**************************************************************************
  //** removeRange
  //**************************************************************************
    private void removeRange(int start, int end) {
        points.subList(start, end).clear();
    }


  //**************************************************************************
  //** computeEdgeList
  //**************************************************************************
    private ArrayList<Edge> computeEdgeList(){
        ArrayList<Edge> edgeList = new ArrayList<>();
        Point a = points.get(points.size()-1);
        for (int i=0; i<points.size(); i++){
            Point b = points.get(i);

            if (i==0){
                this.xmin = a.x;
                this.xmax = a.x;
                this.ymin = a.y;
                this.ymax = a.y;
            }
            else{
                if (a.x < this.xmin){
                    this.xmin = a.x;
                }
                if (a.x > this.xmax){
                    this.xmax  = a.x;
                }
                if (a.y < this.ymin){
                    this.ymin = a.y;
                }
                if (a.y > this.ymax){
                    this.ymax  = a.y;
                }
            }

            edgeList.add(new Edge(a,b));
            a = b;
        }
        return edgeList;
    }


  //**************************************************************************
  //** yIntersect
  //**************************************************************************
  /** Returns the y-intercept of an edge for a given x coordinate
   */
    private Integer yIntersect(int x, Edge e){

        if (e.m==null){ //horizonal line
            return null;
        }
        else{
            double yfirst = (e.m) * (x-0.5) + e.b;
            double ylast = (e.m) * (x+0.5) + e.b;

            if (!e.isTop){
                return (int)Math.floor(Math.min(yfirst, ylast));
            }
            else {
                return (int)Math.ceil(Math.max(yfirst, ylast));
            }
        }
    }


  //**************************************************************************
  //** xIntersect
  //**************************************************************************
  /** Returns the x-intercept of an edge for a given y coordinate
   */
    private int xIntersect(int y, ArrayList<Edge> edgeList){
        int x=0;
        for (Edge e : edgeList){
            if (e.isRight && e.ymin <= y && e.ymax >= y){
                if (e.m==null){
                    x = e.xmin;
                }
                else{
                    double x0 = (double)(y+0.5 - e.b)/e.m;
                    double x1 = (double)(y-0.5 - e.b)/e.m;
                    x = (int)Math.floor(Math.min(x0,x1));
                }
            }
        }
        return x;
    }


  //**************************************************************************
  //** findEdge
  //**************************************************************************
    private Edge findEdge(int x, boolean isTop, ArrayList<Edge> edgeList){
        ArrayList<Edge> edges = new ArrayList<>();
        for (Edge e : edgeList){
            if (e.xmin == x){
                if ((e.isTop && isTop)||(!e.isTop && !isTop)){
                    edges.add(e);
                }
            }
        }
        if (edges.size()==1) return edges.get(0);
        for (Edge e : edges){
            if (e.xmax == e.xmin){
                return e;
            }
        }
        return edges.get(edges.size()-1);
    }


  //**************************************************************************
  //** computeLargestRectangle
  //**************************************************************************
  /** Used to find the largest rectangle that will fit inside a convex hull.
   *  This method uses a brute force algorithm to perform an exhaustive search
   *  for a solution.
   */
    private Rectangle computeLargestRectangle(){
        if (points.size()<3) return null;
        int[] r = new int[]{0,0,0,0};

      //Get list of edges that form the convex hull
        ArrayList<Edge> edgeList = computeEdgeList();


      //Find first top and bottom edges of the convex hull. The top edge forms
      //a 0-90 deg angle from minx. The bottom edge forms a 90-180 degree angle
      //from minx
        Edge topEdge = findEdge(xmin, true, edgeList);
        Edge bottomEdge = findEdge(xmin, false, edgeList);



      //Precompute a list of x-intercepts for every possible y coordinate value
        ArrayList<Point> xIntercepts = new ArrayList<>();
        for(int y=0; y<ymax; y++){
            int x = xIntersect(y, edgeList);
            xIntercepts.add(new Point(x,y));
        }


      //Scan for rectangle starting from the left-most position of the convex hull
        for (int x=xmin; x<xmax; x++){


          //Find y-intercept for the top and bottom edges
            Integer top = yIntersect(x, topEdge);
            Integer bottom = yIntersect(x, bottomEdge);
            if (bottom==null){ //bottomEdge is vertical
                bottom = bottomEdge.ymax;
            }


          //Step through the y-intercepts
            for (int y=bottom; y>=top; y--){//y = y-intercept from bottom to top
                for (int y1=top; y1<bottom; y1++){//y1 = y-intercept from top to bottom
                    if (y1>y){


                      //Find right side (x-intercept of an edge to the right of the current position)
                        int x1 = max(xIntercepts.get(y).x, 0);
                        int x2 = max(xIntercepts.get(y1).x, 0);
                        int right = min(x1, x2);


                      //Update rectangle
                        if (right>0){
                            int height = y1-y;
                            int width = right-x;
                            int area = width * height;
                            if (area>(r[2]*r[3])){
                                r[0] = x;
                                r[1] = y;
                                r[2] = width;
                                r[3] = height;
                            }
                        }
                    }
                }
            }


            if (x == topEdge.xmax){
                topEdge = findEdge(x, true, edgeList);
            }

            if (x == bottomEdge.xmax){
                bottomEdge = findEdge(x, false, edgeList);
            }
        }


        return new Rectangle(r[0], r[1], r[2], r[3]);
    }


  //**************************************************************************
  //** Edge
  //**************************************************************************
    private class Edge {
        int xmin, xmax; /* horiz, +x is right */
        int ymin, ymax; /* vertical, +y is down */
        Double m,b; /* y = mx + b */
        boolean isTop, isRight; /* position of edge w.r.t. hull */
        Point p, q;

        public Edge(Point p, Point q){
            this.xmin = min(p.x, q.x);
            this.xmax = max(p.x, q.x);
            this.ymin = min(p.y, q.y);
            this.ymax = max(p.y, q.y);

            if (p.x!=q.x){
                m = ((double)(q.y-p.y))/((double)(q.x-p.x)); //slope
                b = p.y - m*(p.x); //y-intercept
            }
            else{
                //vertical line so no slope and no y intercept
            }

            this.isTop = p.x > q.x; //edge from right to left (ccw)
            this.isRight = p.y > q.y; //edge from bottom to top (ccw)
            this.p = p;
            this.q = q;
        }

        public String toString(){
            return p + "->" + q;
        }
    }


  //**************************************************************************
  //** min
  //**************************************************************************
    private int min(int a, int b){
        if(a<=b) return a; else return b;
    }


  //**************************************************************************
  //** max
  //**************************************************************************
    private int max(int a, int b){
        if (a>=b) return a; else return b;
    }


  //**************************************************************************
  //** getAspectRatio
  //**************************************************************************
    private static double getAspectRatio(double w, double h){
        if (w>h) return h/w; else return w/h;
    }


  //**************************************************************************
  //** getAspectRatio
  //**************************************************************************
    private static double getAspectRatio(Rectangle rect){
        return getAspectRatio(rect.width, rect.height);
    }


  //**************************************************************************
  //** ConvexHull Class
  //**************************************************************************
  /** Used to compute the convex hull of a polygon
   */
    public class ConvexHull {

        private Polygon polygon;
        private Polygon hull;

        public ConvexHull(Polygon polygon){
            this.polygon = polygon;
            ArrayList<Point> list = new ArrayList<>();
            for (int i=0; i<polygon.npoints; i++){
                int x = polygon.xpoints[i];
                int y = polygon.ypoints[i];
                list.add(new Point(x, y));
            }
            hull = new Polygon();
            for (Point point : makeHull(list)){
                hull.addPoint((int)Math.round(point.x), (int)Math.round(point.y));
            }
        }


        public Polygon getPolygon(){
            return polygon;
        }


        public Polygon getHull(){
            return hull;
        }



      /** Returns a new list of points representing the convex hull of the given
       *  set of points. The convex hull excludes collinear points. This algorithm
       *  runs in O(n log n) time.
       */
        private List<Point> makeHull(List<Point> points) {
            List<Point> newPoints = new ArrayList<>(points);
            Collections.sort(newPoints);
            return makeHullPresorted(newPoints);
        }



      /** Returns the convex hull, assuming that each points[i] <= points[i + 1].
       *  Runs in O(n) time.
       */
        private List<Point> makeHullPresorted(List<Point> points) {
            if (points.size() <= 1) return new ArrayList<>(points);

            // Andrew's monotone chain algorithm. Positive y coordinates correspond to "up"
            // as per the mathematical convention, instead of "down" as per the computer
            // graphics convention. This doesn't affect the correctness of the result.

            List<Point> upperHull = new ArrayList<>();
            for (Point p : points) {
                while (upperHull.size() >= 2) {
                    Point q = upperHull.get(upperHull.size() - 1);
                    Point r = upperHull.get(upperHull.size() - 2);
                    if ((q.x - r.x) * (p.y - r.y) >= (q.y - r.y) * (p.x - r.x))
                        upperHull.remove(upperHull.size() - 1);
                    else
                        break;
                }
                upperHull.add(p);
            }
            upperHull.remove(upperHull.size() - 1);

            List<Point> lowerHull = new ArrayList<>();
            for (int i = points.size() - 1; i >= 0; i--) {
                Point p = points.get(i);
                while (lowerHull.size() >= 2) {
                        Point q = lowerHull.get(lowerHull.size() - 1);
                        Point r = lowerHull.get(lowerHull.size() - 2);
                        if ((q.x - r.x) * (p.y - r.y) >= (q.y - r.y) * (p.x - r.x))
                            lowerHull.remove(lowerHull.size() - 1);
                        else
                            break;
                }
                lowerHull.add(p);
            }
            lowerHull.remove(lowerHull.size() - 1);

            if (!(upperHull.size() == 1 && upperHull.equals(lowerHull)))
                upperHull.addAll(lowerHull);
            return upperHull;
        }



        private class Point implements Comparable<Point> {

            public final double x;
            public final double y;


            public Point(double x, double y) {
                this.x = x;
                this.y = y;
            }


            public String toString() {
                return String.format("Point(%g, %g)", x, y);
            }


            public boolean equals(Object obj) {
                if (!(obj instanceof Point))
                        return false;
                else {
                    Point other = (Point)obj;
                    return x == other.x && y == other.y;
                }
            }


            public int hashCode() {
                return Objects.hash(x, y);
            }


            public int compareTo(Point other) {
                if (x != other.x)
                    return Double.compare(x, other.x);
                else
                    return Double.compare(y, other.y);
            }

        }
    }
}