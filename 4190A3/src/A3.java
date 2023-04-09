/**
 * Tabular MDP and RL
 *
 * The purpose of this assignment is to do MDP learning and Q-learning on a tabular environment.
 * The configuration of the grid is given in a file, which is passed to the program via the first argument.
 * The program prints out the state of the program at certain steps during execution based off of queries give in a file as the second argument.
 * **/

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.io.File; // Import the File class
import java.io.FileNotFoundException; // Import this class to handle errors
import java.util.Scanner; // Import the Scanner class to read text files
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import java.util.Random;

/**
 * Do MDP and Q-learning on the grid
 * **/
public class A3 {

    static Random random = new Random();
    private static Problem problem;
    private static LinkedList<Query> queries;
    private static LinkedList<cachedQuery>  cachedQGrid;
    private static LinkedList<cachedQuery>  cachedMDPGrid;

    public static void main(String args[]) {
            
        problem = getGridProblem(args[0]);
        queries = getQueries(args[1]);

        cachedQGrid = new LinkedList<>();
        cachedMDPGrid = new LinkedList<>();


        /**
         * Solve MDP and Q-Learning
         * **/


        solveMDP();
        solveQLearning();

        /**
         * Answer Queries
         * **/

        System.out.println("\n\n\nPRINTING QUERY RESULTS\n---------------------------------------");
        printQueryResults();

    }

    /**
    * Functions for solving Q-learning
    * **/


    /**
     * Runs the specified number of episodes of Q-learning.
     * Caches grid states that will be used to answer queries.
     * **/
    public static void solveQLearning(){
        var grid = constructQLearning();

        for(int i = 0; i < problem.episodes; i++) {
            grid = updateQLearning(grid);
            cacheQGridForQuery(i, grid);
        }

        System.out.println("\n-----  Q-LEARNING SOLUTION  -----\n");
        printTableWide(grid);
    }

    /**
     * Runs one episode of Q-learning
     * input: grid at the start of the episode
     * output: grid at the end of the episode
     * **/
    public static QTile[][] updateQLearning(QTile[][] oldGrid){

        var grid = cpyQGrid(oldGrid);
        var currentState = grid[problem.startState[0]][problem.startState[1]];

        while(!currentState.isTerminal){
            var action = getPolicy(currentState);
            var newState = transition(action, new int[]{currentState.row, currentState.col}, grid);
            var currValue = getQValue(currentState, action);

            var sample = newState.reward + problem.discount*newState.value();
            var newValue = (1-problem.alpha)*currValue + problem.alpha*sample;
            updateQValue(currentState, action, newValue);
            currentState = newState;

        }


        return grid;
    }

    /**
     * updates a Q-value of tiles in the grid
     * input:
     * the tile being updated,
     * the action associated with the q-value being updated,
     * the new Q-value
     * **/
    public static void updateQValue(QTile tile, Direction dir, double value){
        if(dir == Direction.WEST)
            tile.west = value;
        else if(dir == Direction.EAST)
            tile.east = value;
        else if(dir == Direction.SOUTH)
            tile.south = value;
        else if(dir == Direction.NORTH)
            tile.north = value;
    }

    /**
     * get the Q-value of a tile for a given action
     * input:
     * the tile being queried,
     * the action associated with the q-value,
     * output:
     * the Q-value for that state and action
     * **/
    public static double getQValue(QTile tile, Direction dir){
        if(dir == Direction.WEST)
            return tile.west;
        else if(dir == Direction.EAST)
            return tile.east;
        else if(dir == Direction.SOUTH)
            return tile.south;
        return tile.north;
    }

    /**
     * Transition state based off current state and an action
     * input:
     * the action being done,
     * the current state,
     * the grid of states
     * output:
     * the new state after doing the action
     * **/
    public static QTile transition(Direction action, int[] currentState, QTile[][] grid){
        var options = movementSquares(currentState, grid, action);
        double fwdProb = 1-problem.noise;
        double leftProb = fwdProb+ (problem.noise/2);


        var randomValue = random.nextDouble();

        if(randomValue < fwdProb)
            return options[0];
        else if(randomValue < leftProb)
            return  options[1];

        return options[2];

    }

    /**
     * Get the action to based off of the current state.
     * Does a random action with probability epsilon to encourage exploration
     * input:
     * the tile that is the current state,
     * output:
     * the action to do based off the policy
     * **/
    public static Direction getPolicy(QTile currentState){
        var epsilon = .2;
        var randomValue = random.nextDouble();

        if(randomValue < epsilon)
            return randomMove();

        return currentState.getAction();
    }

    /**
     * Get a random action
     * output:
     * a random action
     * **/
    public static Direction randomMove(){
        var randomValue = random.nextDouble();
        if(randomValue < .25)
            return Direction.EAST;
        if(randomValue<.5)
            return Direction.NORTH;
        if(randomValue < .75)
            return Direction.SOUTH;

        return Direction.WEST;

    }

    /**
     * copy the Q-grid
     * input:
     * the Q-grid to be copied
     * output:
     * a copy of the Q-grid
     * **/
    public static QTile[][] cpyQGrid(QTile[][] oldGrid){
        var newGrid = new QTile[problem.vertical][problem.horizontal];

        for(int i = 0; i < problem.vertical; i++)
            for(int j = 0; j < problem.horizontal; j++)
                newGrid[i][j] = oldGrid[i][j].cpy();
        return newGrid;
    }

    /**
     * construct the Q-grid
     * output:
     * the Q-grid in the initial state
     * **/
    public static QTile[][] constructQLearning(){
        var grid = new QTile[problem.vertical][problem.horizontal];

        for(int i = 0; i < problem.vertical; i++)
            for(int j = 0; j < problem.horizontal; j++)
                grid[i][j] = new QTile(problem.transitionCost,i,j);

        for(var terminal : problem.terminalStates){
            grid[terminal[0]][terminal[1]].isTerminal = true;
            grid[terminal[0]][terminal[1]].terminalValue = terminal[2];
        }

        for(var boulder : problem.boulderStates){
            grid[boulder[0]][boulder[1]].isBoulder = true;
        }
        return grid;
    }

    /**
     * Calculate the set of Q-tiles possible to transition to based off of the current position
     * and an action from that position
     * input:
     * the current location
     * the Q-problem grid
     * the action that is being done
     * output:
     * the actions that are possible when doing that action
     * when in this position on the grid
     * **/
    public static QTile[] movementSquares(int[] location, QTile[][] grid, Direction direction){
        var forward = new int[2];
        var right = new int[2];
        var left = new int[2];

        if (direction == Direction.NORTH){
            forward[0] = location[0]-1;
            forward[1] = location[1];

            right[0] = location[0];
            right[1] = location[1]+1;

            left[0] = location[0];
            left[1] = location[1]-1;

        }
        else if(direction == Direction.SOUTH){
            forward[0] = location[0]+1;
            forward[1] = location[1];

            right[0] = location[0];
            right[1] = location[1]-1;

            left[0] = location[0];
            left[1] = location[1]+1;

        }
        else if(direction == Direction.EAST){
            forward[0] = location[0];
            forward[1] = location[1]+1;

            right[0] = location[0]+1;
            right[1] = location[1];

            left[0] = location[0]-1;
            left[1] = location[1];

        }
        else if(direction == Direction.WEST){
            forward[0] = location[0];
            forward[1] = location[1]-1;

            right[0] = location[0]-1;
            right[1] = location[1];

            left[0] = location[0]+1;
            left[1] = location[1];

        }

        if(!isValidMovement(forward[0], forward[1], grid)){
            forward[0] = location[0];
            forward[1] = location[1];
        }
        if(!isValidMovement(left[0], left[1], grid)){
            left[0] = location[0];
            left[1] = location[1];
        }
        if(!isValidMovement(right[0], right[1], grid)){
            right[0] = location[0];
            right[1] = location[1];
        }

        var returnVal = new QTile[]{grid[forward[0]][forward[1]], grid[right[0]][right[1]], grid[left[0]][left[1]]};

        return returnVal;
    }

    /**
     * check if a tile is obstructed by a boulder or edge of the grid
     * input:
     * i: the row of the tile
     * j: the column of the tile
     * output:
     * returns true if the movement is possible, false otherwise
     * **/
    public static boolean isValidMovement(int i, int j, QTile[][] grid){
        var valid = true;
        if(i > grid.length-1 || j > grid[0].length-1 || i < 0 || j < 0)
            valid = false;
        else if(grid[i][j].isBoulder)
            valid = false;
        return valid;
    }


    /**
     *  MDP VALUE INTERATION FUNCTIONS
     *
     * **/

    /**
     * Runs the specified number of iterations of MDP learning.
     * Caches grid states that will be used to answer queries.
     * **/
    public static void solveMDP(){
        var grid = constructMDP();

        for(int i = 0; i < problem.k; i++) {
            grid = iterateGrid(grid);
            cacheMDPGridForQuery(i, grid);
        }

        System.out.println("\n-----  MDP SOLUTION  -----\n");
        printTable(grid);

    }

    /**
     * Runs one iteration of MDP learning
     * input: grid at the start of the iteration
     * output: grid at the end of the iteration
     * **/
    public static MDPTile[][] iterateGrid(MDPTile[][] oldGrid){
        var newGrid = constructMDP();

        for(int i = 0; i < problem.vertical; i++)
            for(int j = 0; j < problem.horizontal; j++)
                if(!newGrid[i][j].isBoulder && !newGrid[i][j].isTerminal){
                    newGrid[i][j].value = computeActionFromValues(new int[]{i,j}, oldGrid).value;
                }
        return newGrid;
    }

    /**
     * Compute the expectimax value of an action for a state
     * input:
     * the state to take the expectimax of
     * the problem state
     * output: an action/state tuple of the highest value action
     * **/
    public static tuple computeActionFromValues(int[] location, MDPTile[][] grid){
        double value = Double.NEGATIVE_INFINITY;

        Direction action = Direction.NORTH;

        //Take the max value over all actions
        for(var dir : Direction.values()){
            var moveValue = valueOfMove(location, grid, dir);
            if(moveValue > value){
                value = moveValue;
                action = dir;
            }
        }
        return new tuple(action,value);
    }

    /**
     * Compute the value of a move in a given state
     * input:
     * the state to get the value of
     * the action being done
     * the problem state
     * output: the value of that action in that state
     * **/
    public static double valueOfMove(int[] location, MDPTile[][] grid, Direction direction){
        double value = 0;
        double plannedMoveProb = 1.0- problem.noise;
        double notPlannedMoveProb = problem.noise/2;

        var movement = movementSquares(location, grid, direction);

        //Sum across possible states
        value += plannedMoveProb*(movement[0].reward + problem.discount*movement[0].value);
        value += notPlannedMoveProb*(movement[1].reward + problem.discount*movement[1].value);
        value += notPlannedMoveProb*(movement[2].reward + problem.discount*movement[2].value);

        return value;
    }


    /**
     * Calculate the set of MDP-tiles possible to transition to based off of the current position
     * and an action from that position
     * input:
     * the current location
     * the MDP problem grid
     * the action that is being done
     * output:
     * the actions that are possible when doing that action
     * when in this position on the grid
     * **/
    public static MDPTile[] movementSquares(int[] location, MDPTile[][] grid, Direction direction){
        var forward = new int[2];
        var right = new int[2];
        var left = new int[2];

        if (direction == Direction.NORTH){
            forward[0] = location[0]-1;
            forward[1] = location[1];

            right[0] = location[0];
            right[1] = location[1]+1;

            left[0] = location[0];
            left[1] = location[1]-1;

        }
        else if(direction == Direction.SOUTH){
            forward[0] = location[0]+1;
            forward[1] = location[1];

            right[0] = location[0];
            right[1] = location[1]-1;

            left[0] = location[0];
            left[1] = location[1]+1;

        }
        else if(direction == Direction.EAST){
            forward[0] = location[0];
            forward[1] = location[1]+1;

            right[0] = location[0]+1;
            right[1] = location[1];

            left[0] = location[0]-1;
            left[1] = location[1];

        }
        else if(direction == Direction.WEST){
            forward[0] = location[0];
            forward[1] = location[1]-1;

            right[0] = location[0]-1;
            right[1] = location[1];

            left[0] = location[0]+1;
            left[1] = location[1];

        }

        if(!isValidMovement(forward[0], forward[1], grid)){
            forward[0] = location[0];
            forward[1] = location[1];
        }
        if(!isValidMovement(left[0], left[1], grid)){
            left[0] = location[0];
            left[1] = location[1];
        }
        if(!isValidMovement(right[0], right[1], grid)){
            right[0] = location[0];
            right[1] = location[1];
        }

        return new MDPTile[]{grid[forward[0]][forward[1]], grid[right[0]][right[1]], grid[left[0]][left[1]]};
    }

    /**
     * check if a tile is obstructed by a boulder or edge of the grid
     * input:
     * i: the row of the tile
     * j: the column of the tile
     * output:
     * returns true if the movement is possible, false otherwise
     * **/
    public static boolean isValidMovement(int i, int j, MDPTile[][] grid){
        var valid = true;
        if(i > grid.length-1 || j > grid[0].length-1 || i < 0 || j < 0)
            valid = false;
        else if(grid[i][j].isBoulder)
            valid = false;
        return valid;
    }

    /**
     * construct the MDP-grid
     * output:
     * the MDP-grid in the initial state
     * **/
    public static MDPTile[][] constructMDP(){
        var grid = new MDPTile[problem.vertical][problem.horizontal];

        for(int i = 0; i < problem.vertical; i++)
            for(int j = 0; j < problem.horizontal; j++)
                grid[i][j] = new MDPTile(problem.transitionCost,i,j);

        for(var terminal : problem.terminalStates){
            grid[terminal[0]][terminal[1]].isTerminal = true;
            grid[terminal[0]][terminal[1]].value = terminal[2];
        }

        for(var boulder : problem.boulderStates){
            grid[boulder[0]][boulder[1]].isBoulder = true;
        }
        return grid;
    }

    /**
    * I/O Functions
    *
    **/


    /**
     * Print the answers to every query
     * **/
    public static void printQueryResults(){
        for(var q : cachedMDPGrid)
            printQueryAnswers(q);

        for(var q : cachedQGrid)
            printQueryAnswers(q);
    }

    /**
     * Cache a grid for an RL query
     * input:
     * the step being cached
     * the problem state
     * **/
    public static void cacheQGridForQuery(int iteration, QTile[][] grid){

        for (var query : queries){
            if(query.method.equals("RL") && query.steps == iteration){
                cachedQGrid.add(new cachedQuery(query, null, grid));
            }
        }

    }

    /**
     * Cache a grid for an MDP query
     * input:
     * the step being cached
     * the problem state
     * **/
    public static void cacheMDPGridForQuery(int iteration, MDPTile[][] grid){
        for (var query : queries){
            if(query.method.equals("MDP") && query.steps == iteration){
                cachedMDPGrid.add(new cachedQuery(query, grid, null));
            }
        }
    }

    /**
     * Print a table of objects
     * input:
     * a table of objects
     * **/
    public static void printTable(Object[][] table){
        var formattingStr = "";

        for(int i = 0; i < table[0].length;i++)
            formattingStr += "%15s";

        for (final Object[] row : table) {
            System.out.format(formattingStr+"%n", row);
        }
    }

    /**
     * Print a table of objects with wide formatting
     * input:
     * a table of objects
     * **/
    public static void printTableWide(Object[][] table){
        var formattingStr = "";

        for(int i = 0; i < table[0].length;i++)
            formattingStr += "%42s";

        for (final Object[] row : table) {
            System.out.format(formattingStr+"%n", row);
        }
    }

    /**
     * Read in the grid problem from a file
     * input:
     * filepath as a string
     * **/
    public static Problem getGridProblem(String fileName){

        int horizontal = Integer.MAX_VALUE;
        int vertical = Integer.MAX_VALUE;
        LinkedList<int[]> terminalStates = new LinkedList<int[]>();
        LinkedList<int[]> boulderStates = new LinkedList<int[]>();
        int[] startState = new int[2];
        int k = Integer.MAX_VALUE;
        int episodes = Integer.MAX_VALUE;
        double discount = Double.MAX_VALUE;
        double alpha = Double.MAX_VALUE;
        double noise = Double.MAX_VALUE;
        double transitionCost = Double.MAX_VALUE;

        try {
            File myObj = new File(fileName);
            Scanner myReader = new Scanner(myObj);
            while (myReader.hasNextLine()) {
                String data = myReader.nextLine();
                if(data.toLowerCase().contains("horizontal")){
                    String[] parts = data.split("=");
                    horizontal = Integer.parseInt(parts[1]);
                }
                else if(data.toLowerCase().contains("vertical")){
                    String[] parts = data.split("=");
                    vertical = Integer.parseInt(parts[1]);
                }
                else if(data.toLowerCase().contains("terminal")){
                    List<Integer> values = extractNumbersRegexStyle(data);
                    for(int i = 1; i < values.size(); i += 4){
                        int arr[] = new int[3];
                        arr[0] = values.get(i);
                        arr[1] = values.get(i + 1);
                        arr[2] = values.get(i + 2);
                        terminalStates.add(arr);
                    }
                }
                else if(data.toLowerCase().contains("boulder")){
                    List<Integer> values = extractNumbersRegexStyle(data);
                    for(int i = 1; i < values.size(); i += 3){
                        int arr[] = new int[2];
                        arr[0] = values.get(i);
                        arr[1] = values.get(i + 1);
                        boulderStates.add(arr);
                    }
                }
                else if(data.toLowerCase().contains("robotstartstate")){
                    List<Integer> values = extractNumbersRegexStyle(data);
                    startState[0] = values.get(0);
                    startState[1] = values.get(1);


                }
                else if(data.toLowerCase().contains("k")){
                    String[] parts = data.split("=");
                    k = Integer.parseInt(parts[1]);
                }
                else if(data.toLowerCase().contains("episodes")){
                    String[] parts = data.split("=");
                    episodes = Integer.parseInt(parts[1]);
                }
                else if(data.toLowerCase().contains("discount")){
                    String[] parts = data.split("=");
                    discount = Double.parseDouble(parts[1]);
                }
                else if(data.toLowerCase().contains("alpha")){
                    String[] parts = data.split("=");
                    alpha = Double.parseDouble(parts[1]);
                }
                else if(data.toLowerCase().contains("noise")){
                    String[] parts = data.split("=");
                    noise = Double.parseDouble(parts[1]);
                }
                else if(data.toLowerCase().contains("transitioncost")){
                    String[] parts = data.split("=");
                    transitionCost = Double.parseDouble(parts[1]);
                }
            }
        } catch (FileNotFoundException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }

        //-------------------- apply flip to problem
        for(int[] i : terminalStates)
            flipCoordinate(i, vertical);
        for(int[] i : boulderStates)
            flipCoordinate(i, vertical);
        flipCoordinate(startState, vertical);


        Problem problemFromFile = new Problem(
                horizontal,
                vertical,
                terminalStates,
                boulderStates,
                startState,
                k,
                episodes,
                discount,
                alpha,
                noise,
                transitionCost);

        return problemFromFile;
    }

    /**
     * Flip an index about the axis
     * **/
    public static void flipCoordinate(int[] coord, int height){
        var tempValue = coord[0];

        coord[0] = height - 1 - coord[1];
        coord[1] = tempValue;

    }

    /**
     * Extract values from file using regex
     * input:
     * regex to match
     * **/
    public static List<Integer> extractNumbersRegexStyle(String str) {
        List<String> arr = new ArrayList<>();
        Pattern p = Pattern.compile("-?\\d+");
        Matcher m = p.matcher(str);
        while (m.find()) {
            arr.add(m.group());
        }
        return arr.stream()
                .map(Integer::parseInt)
                .collect(Collectors.toList());
    }

    /**
     * Read in the queries from file
     * input: filepath as string
     * **/
    public static LinkedList<Query> getQueries(String fileName){
        LinkedList<Query> queries = new LinkedList<Query>();
        try {
            File myObj = new File(fileName);
            Scanner myReader = new Scanner(myObj);
            while (myReader.hasNextLine()) {
                String data = myReader.nextLine();
                String[] values = data.split(",");
                if(values.length == 5){
                    int column = Integer.parseInt(values[0]);
                    int row = Integer.parseInt(values[1]);;
                    int steps = Integer.parseInt(values[2]);;
                    String method = values[3];
                    String query = values[4];

                    //flip row
                    row = problem.vertical - 1 - row;

                    queries.add(new Query(column, row, steps, method, query, problem.vertical));
                }
            }
        } catch (FileNotFoundException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }

        return queries;
    }

    /**
     * Print answers to query
     * input: the query information that has been cached
     * **/
    public static void printQueryAnswers(cachedQuery cachedQuery){
        if( cachedQuery.query.method.equals("MDP")) {
            if (cachedQuery.query.query.equals("stateValue")) {
                System.out.println("\nBOARD AT STEP " + cachedQuery.query.steps  + "\n------------------------------\n");
                printTable(cachedQuery.mdpgrid);
                System.out.println(cachedQuery.query + " : " + cachedQuery.mdpgrid[cachedQuery.query.row][cachedQuery.query.column].value);
            }
            else if (cachedQuery.query.query.equals("bestPolicy")) {
                //Compute policy from value using 1 step of minimax
                System.out.println("\nBOARD AT STEP " + cachedQuery.query.steps  + "\n------------------------------\n");
                printTable(cachedQuery.mdpgrid);
                System.out.println("\n"+ cachedQuery.query + " : " + computeActionFromValues(new int[]{cachedQuery.query.row,cachedQuery.query.column}, cachedQuery.mdpgrid).action);
            }

        }else{
            if (cachedQuery.query.query.equals("bestQValue")) {
                System.out.println("\nBOARD AT STEP " + cachedQuery.query.steps  + "\n------------------------------\n");
                printTableWide(cachedQuery.qgrid);
                System.out.println(cachedQuery.query + " : " + cachedQuery.qgrid[cachedQuery.query.row][cachedQuery.query.column].value());
            }
            else if (cachedQuery.query.query.equals("bestPolicy")) {
                System.out.println("\nBOARD AT STEP " + cachedQuery.query.steps  + "\n------------------------------\n");
                printTableWide(cachedQuery.qgrid);
                System.out.println(cachedQuery.query + " : " +cachedQuery.qgrid[cachedQuery.query.row][cachedQuery.query.column].getAction());
            }

        }
    }

}

//A class representing a Q-tile
class QTile{

    static Random random = new Random();

    double north;
    double south;
    double west;
    double east;

    double terminalValue;

    double reward;
    boolean isTerminal;
    boolean isBoulder;

    int row;
    int col;
    public QTile(double reward, int row, int col){

        north=0;
        south=0;
        west=0;
        east=0;
        terminalValue=0;
        isTerminal = false;
        isBoulder = false;
        this.reward = reward;
        this.row = row;
        this.col = col;
    }

    //return a copy of this file
    public QTile cpy(){
        var newTile = new QTile(reward,row,col);
        newTile.south=south;
        newTile.north=north;
        newTile.west=west;
        newTile.east=east;
        newTile.terminalValue=terminalValue;
        newTile.isTerminal= isTerminal;
        newTile.isBoulder= isBoulder;
        return newTile;
    }

    //return the value of this tile
    public double value(){
        if(isTerminal)
            return terminalValue;

        var val = north;
        if(south > val)
            val = south;
        if(east > val)
            val = east;
        if(west > val)
            val = west;

        return val;
    }

    //get the best action for this tile
    //randomly break ties
    public Direction getAction(){
        var actionList = new LinkedList<Direction>();

        actionList.add(Direction.NORTH);
        var val = north;

        if(west == val)
            actionList.add(Direction.WEST);
        else if(west > val){
            actionList = new LinkedList<Direction>();
            actionList.add(Direction.WEST);
            val = west;
        }

        if(east == val)
            actionList.add(Direction.EAST);
        else if(east > val){
            actionList = new LinkedList<Direction>();
            actionList.add(Direction.EAST);
            val = east;
        }

        if(south == val)
            actionList.add(Direction.SOUTH);
        else if(south > val){
            actionList = new LinkedList<Direction>();
            actionList.add(Direction.SOUTH);
            val = south;
        }

        var index = random.nextInt(actionList.size());

        return actionList.get(index);
    }

    //return a string representation of this tile
    public String toString(){
        var n = String.format("%.2f",north);
        var e = String.format("%.2f",east);
        var w = String.format("%.2f",west);
        var s = String.format("%.2f",south);

        var str = "| N: " + n + " E: " + e + " S: " + s + " W: " + w + " |";

        if(isBoulder)
            str = "B";
        else if(isTerminal)
            str = "T: " + terminalValue;

        return str;
    }

}

//a class representing a state in the MDP
class MDPTile{
    double value;
    double reward;
    boolean isTerminal;
    boolean isBoulder;

    int row;
    int col;

    public MDPTile(double reward, int row, int col){
        value = 0;
        isTerminal = false;
        isBoulder = false;
        this.reward = reward;
        this.row = row;
        this.col = col;
    }

    //return a string representation of this state
    public String toString(){
        String str = String.format("%.2f",value);


        if(isBoulder)
            str = "B";
        else if(isTerminal)
            str = "T: " + str;

        return str;
    }

}

// a class representing the problem
// holds all the information from the configuration
class Problem {

    public int horizontal;
    public int vertical;
    public LinkedList<int[]> terminalStates;
    public LinkedList<int[]> boulderStates;
    public int[] startState;
    public int k;
    public int episodes;
    public double discount;
    public double alpha;
    public double noise;
    public double transitionCost;

    public Problem(
            int horizontal,
            int vertical,
            LinkedList<int[]> terminalStates,
            LinkedList<int[]> boulderStates,
            int[] startState,
            int k,
            int episodes,
            double discount,
            double alpha,
            double noise,
            double transitionCost){

        this.horizontal = horizontal;
        this.vertical = vertical;
        this.terminalStates = terminalStates;
        this.boulderStates = boulderStates;
        this.startState = startState;
        this.k = k;
        this.episodes = episodes;
        this.discount = discount;
        this.alpha = alpha;
        this.noise = noise;
        this.transitionCost = transitionCost;
    }

    //a string representation of the problem
    public String toString(){
        return "Horizontal: "+ horizontal + "\n"+"Vertical: "+ + vertical + "\n" + "Terminal states: "+ listToString(terminalStates) + "\n" + "Boulder states: " + listToString(boulderStates) + "\n" + "Start state: " + "[" + startState[0] + " " + startState[1] + "]\n" + "k: " +  k + "\n" + "episodes: " + episodes + "\n" + "discount: " + discount + "\n" + "alpha: "+  alpha + "\n" + "noise: " +  noise + "\n" + "transition cost: " + transitionCost;
    }

    //write list as string
    private String listToString(LinkedList<int[]> list){
        String result = "[";
        for(int[] arr : list){
            result += "[";
            for(int i = 0; i < arr.length; i++)
                result += arr[i] + ",";
            result += "]";
        }
        result += "]";
        return result;
    }

}

//a class representing a query to answer about the grid
class Query {

    public int column; //h - horizontal
    public int row; //v - vertical
    public int steps;
    public String method;
    public String query;
    public int vertical;
    public Query(int column, int row, int steps, String method, String query, int vertical){
        this.column = column;
        this.row = row;
        this.steps = steps;
        this.method = method;
        this.query = query;
        this.vertical = vertical;
    }

    //string representation of the query
    public String toString(){
        return "\n The query is:  " +  column + ", " + (vertical - (row + 1)) + ", " + steps + ", " + method + ", " + query;
    }

}

//a query that has been cached with a board state
// allows for easy answering of queries at the end of runtime
class cachedQuery {
    public QTile[][] qgrid;
    public MDPTile[][] mdpgrid;
    public Query query;

    public cachedQuery(Query q, MDPTile[][] mdpgrid, QTile[][] qgrid){
        this.qgrid = qgrid;
        this.mdpgrid = mdpgrid;
        query = q;
    }
}

// an action and value tuple
class tuple{
    Direction action;
    double value;

    public tuple(Direction action, double value){
        this.action = action;
        this.value = value;
    }

    //string representation of the tuple
    public String toString(){
        return "Direction : " + action + "  value: " + value;
    }
}

//Enum of all possible actions
enum Direction {
    NORTH,
    EAST,
    WEST,
    SOUTH
}
