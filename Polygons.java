/*
Maxwell King-Wilson
1/20/2013
This project calculates the shortest path through a maze of convex polygons.
 */



import javax.swing.*;
import java.awt.*;
import java.awt.Graphics;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.lang.Math;
import java.awt.geom.Line2D;
import java.util.List;
import java.util.Stack;




public class Polygons{


    private static String sPoint, ePoint;
    public static double Startx, Starty;
    public static double GStartx = 0;
    public static double GStarty = 0;
    public void setStartPoint(String c) {
        sPoint = c;
    }

    public String getStartPoint() {
        return sPoint;
    }

    public void setEndPoint(String c) {
        ePoint = c;
    }

    public String getEndPoint() {
        return ePoint;
    }

    public void setStartx(){
        Startx = 0;
    }

    public double getStartx(){
        return Startx;
    }

    public void setStarty(){
        Starty = 0;
    }

    public double getStarty(){
        return Starty;
    }



    public static void main(String[] args) throws IOException {



//First reads in the lines from the .txt file.  The first and second lines determine the start and ending point of the maze.
//The rest of the lines give the locations of the corners of each convex polygon in the maze.



//        Example map input:
//        1, 3
//        34, 19
//        0, 14; 6, 19; 9, 15; 7, 8; 1, 9
//        2, 6; 17, 6; 17, 1; 2, 1
//        12, 15; 14, 8; 10, 8
//        14, 19; 18, 20; 20, 17; 14, 13
//        18, 10; 23, 6; 19, 3
//        22, 19; 28, 19; 28, 9; 22, 9
//        25, 6; 29, 8; 31, 6; 31, 2; 28, 1; 25, 2
//        31, 19; 34, 16; 32, 8; 29, 17



        //Open specified text file
        List<String> list = new ArrayList<String>();
        File library = new File(args[0]);
        Scanner sc = new Scanner(library);

        //Reads in every line of the text file
        //creates a new String for every line, then adds it to an arraylist
        while(sc.hasNextLine()) {
            String line;
            line = sc.nextLine();
            list.add(line);
        }

        //Takes the first and second lines as the start and goal points
        int count = 0;

        sPoint = list.get(0);
        ePoint = list.get(1);

        sPoint = sPoint.replace(" ", "");
        ePoint = ePoint.replace(" ", "");

        //remove the first line from the polygon list
        list.remove(0);

        //Create a 2 element array for the start point
        String[] Starter = sPoint.split(",");
        String[] Ender = ePoint.split(",");
        Startx = Double.parseDouble(Starter[0]);
        Starty = Double.parseDouble(Starter[1]);



        Polygon[] FinalPolygons = ListPolygons(list);


        //Text Graph of the polygon maze and the Start/End points
        graph(FinalPolygons);

        //A* search algorithm is performed to find the shortest path
        AStarSearch(FinalPolygons);
    }



    private static List<Points> addStuff (List<Points> openSet,Points Neighbor){

        boolean dummy = true;
        int count = 0;

        //create a dummy Point to be attached at the start of the openset list
        Points sortingDummy = new Points(999999,0,-1,-1,-1,0,0);


        if(dummy){
            openSet.add(0, sortingDummy);
            dummy = false;
        }

        //order the points in the openset by f-score
        for ( int j=0; j<openSet.size();j++){
            if(openSet.get(j).costF > Neighbor.costF){
                openSet.add(j, (Neighbor));
                count++;
                break;
            }
        }

        if(count == 0){
            openSet.add(Neighbor);
        }


        //remove dummy Points after sorting is complete
        while(openSet.contains(sortingDummy)){
            openSet.remove(sortingDummy);
        }

        //return the open set
        return openSet;
    }


    private static Polygon[] ListPolygons(List<String> list) {
        List<String[]> DeclaredPolyList = RemoveSpaces(list);
        Polygon[] PolygonList = new Polygon[DeclaredPolyList.size()];
        for (int j=0; j< DeclaredPolyList.size(); j++)
        {
            //create arrays of the x and y points of the polygon
            int[] xpoints = new int[DeclaredPolyList.get(j).length];
            int[] ypoints = new int[DeclaredPolyList.get(j).length];




            for (int i = 0; i < xpoints.length; i++)
            {
                //add the xpoints and y points from each array into polygon objects to construct the corner locations
                String[] result = DeclaredPolyList.get(j)[i].split(",");
                xpoints[i] = Integer.parseInt(result[0]);
                ypoints[i] = Integer.parseInt(result[1]);
            }

            //Add the constructed polygon to an array of polygons
            PolygonList[j] = new Polygon(xpoints,ypoints,xpoints.length);
        }

        //return polygons
        return PolygonList;
    }


    //This method splits up the strings from each line of the map text file and removes spaces therein
    public static List<String[]> RemoveSpaces(List<String> list)
    {
        List<String[]> addresses = new ArrayList<String[]>();
        List<String> polylist = new ArrayList<String>();
        for (int i = 0; i < list.size(); ++i)
        {
            String shape;
            shape = list.get(i).replace(" ", "");
            polylist.add(shape);
            String[] result = polylist.get(i).split(";");
            addresses.add(result);
        }
        return addresses;
    }


    //Calculates the distance between two points
    private static double distance(double x1, double y1, double x2, double y2)
    {
        return Math.sqrt((x1-x2)*(x1-x2) + (y1-y2)*(y1-y2));
    }

    //The "agent" can only travel from one corner of a polygon to another
    //Method for calculating which points on the graph are visible, and which are not
    //This method checks every corner of every polygon to see where

    private static List<Points> visible(Polygon[] finalPolygons){


        //Arraylist of visible points
        List<Points> visiblePoints = new ArrayList<Points>();

        //increment for every time a point is considered "not visible" from the current starting point
        int notVisible = 0;
        int polycount = -1;

        //Here we loop through every constructed polygon to see if any may be blocking the path to the next point
        for(Polygon currentPolygon : finalPolygons){
            polycount++;

            //Here we create a line between the current point (Startx,Starty) and any potential destination on the board
            //We check if anything gets between the start point and the potential destination

            //We loop through every corner in the current polygon
            for (int i = 0; i < currentPolygon.xpoints.length; i++){


                boolean Connected = false;
                //First a line is created here between the starting point and the potential destination
                //The program will use many tests to make sure that there is a clear line between the start and destination

                //i = destination corner
                Line2D.Double myLine = new Line2D.Double(Startx, Starty, currentPolygon.xpoints[i], currentPolygon.ypoints[i]);

                //This loop tests to see if the current starting point and destination are on the same polygon, and if they are, are the two points connected by the same edge
                for(int k = 0; k < currentPolygon.xpoints.length; k++ ){
                    /*This set of if statements avoids out of bounds exceptions by checking if the starting or destination points are the first
                     or last corners declared in the array of each pol
                      */

                    //If the starting point is on the current polygon being checked
                    if ((Startx == currentPolygon.xpoints[k] && Starty == currentPolygon.ypoints[k])){
                        //If the current destination point is the last corner listed on the current polygon
                        if(i == currentPolygon.xpoints.length-1){
                            //If the current point is the last or first corner listed on the current polygon
                            if(((Startx == currentPolygon.xpoints[currentPolygon.xpoints.length - 2] && Starty == currentPolygon.ypoints[currentPolygon.xpoints.length -2]))
                                    || ((Startx == currentPolygon.xpoints[0] && Starty == currentPolygon.ypoints[0]))){
                                //The destination corner is visible
                                Connected = true;
                            }
                            //if the destination corner is the first corner on the current polygon
                        } else if (i == 0){
                            //if the starting point is last or second corner listed on the polygon's corner array
                            if(((Startx == currentPolygon.xpoints[currentPolygon.xpoints.length - 1] && Starty == currentPolygon.ypoints[currentPolygon.xpoints.length - 1])) || ((Startx == currentPolygon.xpoints[1] && Starty == currentPolygon.ypoints[1]))){


                                Connected = true;
                            }

                            //If the destination point is neither the first or last corner on the polygon, check if the starter point is connected to the destination on the polygon
                        } else if((Startx == currentPolygon.xpoints[i-1] && Starty == currentPolygon.ypoints[i-1]) || (Startx == currentPolygon.xpoints[i+1] && Starty == currentPolygon.ypoints[i+1])){

                            Connected = true;
                        }

                        //If the start and destination points are on the same polygon, but none of the above conditions are met,  then the destination is not visible from the starting point
                        if (!Connected) {
                            notVisible++;
                        }
                    }
                }


                //Check to see if any other polygons block the path between the starting and destination point
                for(Polygon crossPolygon : finalPolygons){

                    //loop through all the corner points on the map
                    for (int j = 0; j < crossPolygon.xpoints.length - 1; j++){

                        //Create a "cross line" to see if it intersects the line between the starting and destination point
                        Line2D.Double crossLine = new Line2D.Double(crossPolygon.xpoints[j], crossPolygon.ypoints[j],crossPolygon.xpoints[j+1], crossPolygon.ypoints[j+1]);

                        //if the destination point is not on either end of the cross line
                        if (((currentPolygon.xpoints[i] != crossPolygon.xpoints[j] || currentPolygon.ypoints[i] != crossPolygon.ypoints[j]))
                                && ((currentPolygon.xpoints[i] != crossPolygon.xpoints[j + 1] || currentPolygon.ypoints[i] != crossPolygon.ypoints[j + 1]))) {
                            //if the starting point is not on either point of the line
                            if (((Startx != crossPolygon.xpoints[j] || Starty != crossPolygon.ypoints[j]))
                                    && ((Startx != crossPolygon.xpoints[j + 1] || Starty != crossPolygon.ypoints[j + 1]))) {

                                //if the cross line intersects with the line between the starting and destination point
                                if(myLine.intersectsLine(crossLine)){
                                    notVisible++;
                                    break;
                                }
                            }
                        }
                    }

                    //to avoid out of bounds exceptions, we repeat the above logic but as special cases for the first and last corners of the "cross polygon" array
                    if(notVisible == 0){
                        if ((currentPolygon.xpoints[i] != crossPolygon.xpoints[crossPolygon.xpoints.length - 1] || currentPolygon.ypoints[i] != crossPolygon.ypoints[crossPolygon.xpoints.length - 1]) && ((currentPolygon.xpoints[i] != crossPolygon.xpoints[0] || currentPolygon.ypoints[i] != crossPolygon.ypoints[0]))) {
                            if ((Startx != crossPolygon.xpoints[crossPolygon.xpoints.length - 1] || Starty != crossPolygon.ypoints[crossPolygon.xpoints.length - 1]) && ((Startx != crossPolygon.xpoints[0] || Starty != crossPolygon.ypoints[0]))) {
                                Line2D.Double extraSide = new Line2D.Double(crossPolygon.xpoints[crossPolygon.xpoints.length -1], crossPolygon.ypoints[crossPolygon.xpoints.length - 1],crossPolygon.xpoints[0], crossPolygon.ypoints[0]);
                                if( myLine.intersectsLine(extraSide)){
                                    notVisible++;
                                }
                            }
                        }
                    }
                }

                //if the corner is deemed not visible, move onto the next
                if (notVisible > 0){
                    notVisible = 0;
                }
                else{
                    //if the corner is visible, then we add it to a list of points
                    double FreeNow = distance(Startx, Starty, currentPolygon.xpoints[i], currentPolygon.ypoints[i]);
                    visiblePoints.add(new Points(currentPolygon.xpoints[i], currentPolygon.ypoints[i], FreeNow, 9999, 9998, 0, 0));
                }
            }
        }

        //return the list of visible points
        return visiblePoints;
    }


//    function A*(start,goal)
//    closedset := the empty set    // The set of nodes already evaluated.
//    openset := {start}    // The set of tentative nodes to be evaluated, initially containing the start node
//    came_from := the empty map    // The map of navigated nodes.
//
//    g_score[start] := 0    // Cost from start along best known path.
//    // Estimated total cost from start to goal through y.
//    f_score[start] := g_score[start] + heuristic_cost_estimate(start, goal)
//
//    while openset is not empty
//    current := the node in openset having the lowest f_score[] value
//    if current = goal
//    return reconstruct_path(came_from, goal)
//
//    remove current from openset
//    add current to closedset
//    for each neighbor in neighbor_nodes(current)
//    if neighbor in closedset
//    continue
//    tentative_g_score := g_score[current] + dist_between(current,neighbor)
//
//    if neighbor not in openset or tentative_g_score < g_score[neighbor]
//    came_from[neighbor] := current
//    g_score[neighbor] := tentative_g_score
//    f_score[neighbor] := g_score[neighbor] + heuristic_cost_estimate(neighbor, goal)
//    if neighbor not in openset
//    add neighbor to openset
//
//    return failure
//
//    function reconstruct_path(came_from, current_node)
//    if current_node in came_from
//    p := reconstruct_path(came_from, came_from[current_node])
//    return (p + current_node)
//            else
//            return current_node


    //This method will construct the best possible path by using the A* search algorithm
    private static List<String> AStarSearch(Polygon[] finalPolygons){


        List<String> completedPath = new ArrayList<String>();
        String[] Ender = ePoint.split(",");

        //The final goal point
        double  endx = Double.parseDouble(Ender[0]);
        double  endy = Double.parseDouble(Ender[1]);

        //////////////////////////////////////////////////
        //Correct path from the example map:
        // (1,3)  (2,6)  (14,8)  (22,19)  (28,19)  (31,19)  (34,19)
        //////////////////////////////////////////////////

        //set of points to be evaluated, initially only containing start point
        List<Points> openset = new ArrayList<Points>();
        //set of points already evaluated
        List<Points> closedset = new ArrayList<Points>();

        //The initial starting point
        GStartx = Startx;
        GStarty = Starty;


        //G-score is the calculated cost from the start to the current point

        //Calculating F score, which is the distance between the current starting point and the final end point
        double F_Cost = distance(Startx, Starty, endx, endy);

        //initializing variables for each point

        //current x and y location, current "came from" x and y location
        int currX, currY,currCamex, currCamey;

        //current G-Score and F-Score
        double currG, currF;


        double tentGscore,tentFscore;
        openset.add(new Points((int)Startx,(int)Starty,0,0,F_Cost,-1,-1));
        boolean dummy = true;

        //while the openset is not empty
        while (openset.size() != 0){


            //get the next Point to be evaluated in the openset with the lowest f-score

            //filling variables with the information from the point being evaluated
            currX = openset.get(0).XPoint;
            currY = openset.get(0).YPoint;
            currF = openset.get(0).costF;
            currG = openset.get(0).costG;
            currCamex = openset.get(0).camefromx;
            currCamey = openset.get(0).camefromy;
            Startx = currX;
            Starty = currY;


            //If the current point equals the goal point, start constructing a path back to the start
            if(currX == endx  && currY == endy){
                //add the last point to the closed set
                closedset.add(new Points(currX,currY,0,currG,currF,currCamex,currCamey));
                for (int r = closedset.size()-1 ; r >= 0; r--){

                    //Loop from the end to the start of the closed set
                    //From the end point, look at the point that was "travelled from" and add that point to the path
                    if((currX == closedset.get(r).XPoint)&&(currY == closedset.get(r).YPoint)){

                        completedPath.add(" (" + currX + "," + currY + ") ");

                        currX = closedset.get(r).camefromx;
                        currY = closedset.get(r).camefromy;
                    }
                }

                //Reverse the order from start to finish and print the path
                Collections.reverse(completedPath);
                System.out.print("Best path: " );
                for(String omg : completedPath){
                    //Final, constructed path
                    System.out.print(omg);
                }

                //End the algorithm
                break;

            }

            //Check which points can be seen and travelled to from the current point
            List<Points> Seeable = visible(finalPolygons);


            //Before evaluating the current point, remove it from the openset and add it to the closed set
            openset.remove(0);
            closedset.add(new Points(currX,currY,0,currG,currF,currCamex,currCamey));


            //loop through all of the seeable points from the current point
            for(Points neighbor : Seeable){
                int wanz = 0;

                //Tentative F-Score = current G Score and the distance between current point and the neighbor point
                tentGscore =  currG + neighbor.lineLength;
                tentFscore = tentGscore + distance(neighbor.XPoint,neighbor.YPoint,endx,endy);


                for(int j = 0; j < closedset.size() - 1; j++){
                    // if neighbor point is in closedset and the tentative FScore is >= the neighbor's FScore
                    if((neighbor.XPoint == closedset.get(j).XPoint)&&(neighbor.YPoint == closedset.get(j).YPoint)){

                        //give the neighbor the F and G score it already has in the closed set
                        neighbor.costF = closedset.get(j).costF;
                        neighbor.costG = closedset.get(j).costG;
                    }
                }



                //if the neighbor point is not in the open set, or the tentative FScore of the current point is less than the FScore of the neighbor point
                if((!openset.contains(neighbor)) || tentFscore < neighbor.costF){

                    //assign the current point as the "travelled from" point for the neighbor point
                    neighbor.camefromx = currX;
                    neighbor.camefromy = currY;

                    //give the F and G scores of the current point to the neighbor point
                    neighbor.costG = tentGscore;
                    neighbor.costF = tentFscore;

                    //Check to see if the current neighbor is in the open set
                    for(int i = 0; i < openset.size() - 1; i++){
                        if((neighbor.XPoint == openset.get(i).XPoint)&&(neighbor.YPoint == openset.get(i).YPoint)){
                            wanz++;
                        }
                    }

                    //If not, add to the open set
                    if(wanz == 0)  {
                        openset = addStuff(openset,neighbor);
                    }
                }
            }
        }
        return completedPath;
    }


    private static void graph(Polygon[] finalPolygons) {
        int weiners =0;
        String[] Starter = sPoint.split(",");
        String[] Ender = ePoint.split(",");
        int  endx = Integer.parseInt(Ender[0]);
        int  endy = Integer.parseInt(Ender[1]);
        int G = 40;
        for(int i=0; i<G; i++) {
            for(int j=0; j<G; j++){
                int loketest = 0;
                if((i ==Startx && j == Starty) || i ==endx && j == endy){
                    loketest = 2;
                }
                for(Polygon helpme : finalPolygons){
                    if(helpme.contains(i, j)){
                        loketest = 1;
                    }
                }
                if(loketest == 1){
                    System.out.print("1");
                }else if(loketest == 2){
                    System.out.print("X");
                }
                else{
                    System.out.print("0");
                }
            }

            System.out.println("     \t");
            weiners++;
        }
    }

    //Points class holds all of the necessary variables for every point on the map
    public static class Points {
        private  int XPoint;
        private  int YPoint;
        private  int camefromx, camefromy;
        private  double lineLength;
        private  double costG;
        private  double costF;

        public Points(int XPoint, int YPoint, double lineLength, double costG, double costF, int camefromx, int camefromy) {
            //X point
            this.XPoint = XPoint;

            //Y point
            this.YPoint = YPoint;

            //Distance between this point and the point "travelled from"
            this.lineLength = lineLength;

            //G Score
            this.costG = costG;

            //F Score
            this.costF = costF;

            //X value of the "travelled from" point
            this.camefromx = camefromx;

            //Y value of the "travelled from" point
            this.camefromy = camefromy;
        }

        public int getXPoint() {
            return XPoint;
        }

        public int getYPoint() {
            return YPoint;
        }

        public double getLineLength(){
            return lineLength;
        }

        public double getCostG(){
            return costG;
        }

        public double getCostF(){
            return costF;
        }

        public int getCamefromx(){
            return camefromx;
        }

        public int getCamefromy(){
            return camefromy;
        }

    }



}

