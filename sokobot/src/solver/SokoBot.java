package solver;

import java.util.*;

public class SokoBot {

  // mapData contains walls (#) and goal boxes (.)
    // itemsData contains boxes ($) and player (@)

    // so we need positions of boxes, player, and goal boxes... will also be used later for heuristic calculation
    private ArrayList<int[]> Position_boxes = new ArrayList<>(); //will be modified during decoding of states
    private ArrayList<int[]> Position_goals = new ArrayList<>();
    private int[] Position_player = new int[2]; //also to be modified during decoding of states

    //auxillaries
    private ArrayList<int[]> Position_deadlocks = new ArrayList<>(); //if box is in any of these positions, do not add to frontier aka prune
    private int box_count = 0; // this will be used for key in hashmap when decoding positions
    private int[][] box_assignments; //box to goal assignments through self-made Hungarian algo

    private HashMap<String, State> ExploredStates = new HashMap<>();
    
    //hashkey is in the format: "px py b1x b1y b2x b2y ..."
    
    //our heuristic is a hashmap of positions to manhattan distances
    //compute manhattan distances from each box to each goal position
  public String solveSokobanPuzzle(int width, int height, char[][] mapData, char[][] itemsData) {
    


    /**
     * We need to:
     * 1. cutoff deadlocks
     * 2. implement manhattan distance heuristic for A* search
     * 3. implement Dynamic Programming to store best Manhattan paths between goal states from boxes
     * 4. Implement Dynamic Programming to store best actions from player to boxes
     * 5. implement A* search
     */
    
    initializeMapData(width, height, mapData, itemsData);
    // do assignment of boxes to goals using Hungarian algo (explanation at Manhattan Distance function)
    HungarianAssignment box_assignment_HA = new HungarianAssignment(this.box_count);
    this.box_assignments = box_assignment_HA.HungarianAlgorithm();
    //format of assignments is {{Row0, Col2}, {Row1, Col1}, {Row2, Col0}, {Row3, Col3}} (example for 4 boxes)
    //so {{Box0, Goal2}, {Box1, Goal1}, {Box2, Goal0}, {Box3, Goal3}}


    /*
     * YOU NEED TO REWRITE THE IMPLEMENTATION OF THIS METHOD TO MAKE THE BOT SMARTER
     */
    /*
     * Default stupid behavior: Think (sleep) for 3 seconds, and then return a
     * sequence
     * that just moves left and right repeatedly.
     */
    try {
      Thread.sleep(1000);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    return "lrlrlrlrlrlrlrlrlrlrlrlrlrlrlrlrlrlrlrlrlrlrlrlrlrlrlrlrlrlrlrlrlrlrlrlrlrlr";
  }

  //Initially I was thinking of doing manhattan distance of each box to every goal, it wasn't efficient, and I thought rin na choosing the min manhattahn idstance for each box to their closest goal is not enough (too much repeated computation). I came across Hungarian algorithm which reduces the computation needed for not needing the repeated finding of minimum distances. I implemented box to goal assigned manhattan distances (that were assigned via Hungarian algo)
  //serves as heuristic function
  // ONLY COMPUTES THE MANHATTAN DISTANCE OF BOXES TO ASSIGNED GOALS, NO PENALTY YET FOR DEADLOCKS
  private int computeManhattanDistances() {
    int totalDistance = 0;
    for (int i = 0; i < box_count; i++) {
      //use this.box_assignments to match the box to their goals for manhattan distance checking
      //this.box_assignments[i][1] gives the goal index assigned to box i
      int[] goal_position = this.Position_goals.get(this.box_assignments[i][1]);
      totalDistance += computeManhattanDistance(goal_position, this.Position_boxes.get(i));
      }
    return totalDistance;
  }

  private int computeManhattanDistance(int[] pos1, int[] pos2) {
    return Math.abs(pos1[0] - pos2[0]) + Math.abs(pos1[1] - pos2[1]);
  }

  //class to represent position
  private class State {
    //String stateKey; //stateKey will be passed down through functions naman, myb no need
    String previousStateKey;
    int totalCostsoFar;

    public State(String previousStateKey, int totalCostsoFar) {
      //this.stateKey = stateKey;
      this.previousStateKey = previousStateKey;
      this.totalCostsoFar = totalCostsoFar;
    }
  }

  private void initializeMapData(int width, int height, char[][] mapData, char[][] itemsData) {
    this.Position_boxes.clear();
    this.Position_goals.clear();
    this.Position_deadlocks.clear();
    this.box_count = 0;
    for (int i = 1; i < height-1; i++) { // only for the playable tiles (because boundary walls) no need to compute
      for (int j = 1; j < width-1; j++) {
        if (mapData[i][j] == ' ') {
          //check for deadlock pos (corner)
          //wall above
          if (mapData[i-1][j] == '#') {
            //upper diagonal left
            if (mapData[i-1][j-1] == '#' && mapData[i][j-1] == '#')
              Position_deadlocks.add(new int[]{i, j});
            //upper diagonal right
            else if (mapData[i-1][j+1] == '#' && mapData[i][j+1] == '#')
            Position_deadlocks.add(new int[]{i, j});
          }
          //wall below
          else if (mapData[i+1][j] == '#') {
            //lower diagonal left
            if (mapData[i+1][j-1] == '#' && mapData[i][j-1] == '#')
              Position_deadlocks.add(new int[]{i, j});
            //lower diagonal right
            else if (mapData[i+1][j+1] == '#' && mapData[i][j+1] == '#')
              Position_deadlocks.add(new int[]{i, j});
          }
        }
        if (mapData[i][j] == '#') continue;
        if (mapData[i][j] == '.') Position_goals.add(new int[]{i, j});
        if (itemsData[i][j] == '$') {
          Position_boxes.add(new int[]{i, j});
          box_count++;
        }
        if (itemsData[i][j] == '@') Position_player = new int[]{i, j};
      }

    }
      
  } // reading of positions, tile penalties (deadlocks), done.

  //case use: HungarianAssignment initialize_box_count = new HungarianAssignment(box_count);
  //int[][] assignments = initialize_box_count.HungarianAlgorithm();  
  public class HungarianAssignment {
    // Manhattan distances matrix
    //in reference to https://github.com/aalmi/HungarianAlgorithm/blob/master/HungarianAlgorithm.java#L204
    int[] Position_box;
    int[] Position_goal;
    int[][] distanceMatrix;

    //zero line row and column trackers
    //zl_row and zl_col track which rows and columns are covered na
    //RowSquares contains the column indices per row index (ex. RowSquares[2] = 1 , means in row idx 2, column index 1 has a square)
    //ColSquares contains the row indices per column index (if it has -1, means no square)
    //RowStars contains the column indices per row index of the main zeroes
    int[] RowStars,RowSquares, ColSquares, zl_row, zl_col;
    int box_count;

    //int[][] BoxAssignments;
    
    
    public HungarianAssignment(int box_count) {
      this.box_count = box_count;
      this.distanceMatrix = new int[this.box_count][this.box_count];
      this.zl_row = new int[this.box_count];
      this.zl_col  = new int[this.box_count];
      this.RowStars = new int[this.box_count];
      this.RowSquares = new int[this.box_count];
      this.ColSquares = new int[this.box_count];
      //this.BoxAssignments = new int[box_count][2];

      Arrays.fill(this.RowStars, -1);
      Arrays.fill(this.RowSquares, -1);
      Arrays.fill(this.ColSquares, -1);
      Arrays.fill(this.zl_row, 0);
      Arrays.fill(this.zl_col, 0);

      // get box-goal distances of each possible pair
      for (int i = 0; i < this.box_count; i++) {
        this.Position_box = Position_boxes.get(i);
        for (int j = 0; j < this.box_count; j++) {
          this.Position_goal = Position_goals.get(j);
          this.distanceMatrix[i][j] = computeManhattanDistance(this.Position_box, this.Position_goal);
        }
      }
    }

    public int[][] HungarianAlgorithm() {
      //Step 1 and 2: subtract row and column minimums
      Step1and2_columnrowreduction();
      
      //Step 3-4: draw minimum num lines to cover all zeros, keep doing until number of lines = box_count 
      Step3_greedySquares();
      Step4_LineMarks();

      while (!ColumnsAreAllCovered()) {
        int[] mainZero = Step5_FindMainZero();
        while (mainZero == null) {
          Step8_AddandSubtract();
          mainZero = Step5_FindMainZero();
        }
        if (RowSquares[mainZero[0]] == -1) {
          Step7_StarAndSquareChain(mainZero);
          Step4_LineMarks();
        } 
        else { // when a square exists in row of main zero
          //Step 6
          zl_row[mainZero[0]] = 1; //put a line on the main zero row
          zl_col[RowSquares[mainZero[0]]] = 0; //keeps the column of main zero open
          //Step8
          Step8_AddandSubtract();
        }
      }

      //optimal assignments
      int[][] HungarianAssignments = new int[this.box_count][2];
      for (int i = 0; i < this.box_count; i++) {
        HungarianAssignments[i] = new int[]{i, RowSquares[i]};
      }

      return HungarianAssignments;
      //the format of the assignments is {{Row0, Col2}, {Row1, Col1}, {Row2, Col0}, {Row3, Col3}}
    }

    private void Step1and2_columnrowreduction() {
      int rowMin, colMin;
        //Step 1: Subtract row minimums
      for (int i = 0; i < this.box_count; i++) {
        rowMin = Integer.MAX_VALUE; //it's OVER 9000!!!
        for (int j = 0; j < this.box_count; j++) {
          if (this.distanceMatrix[i][j] < rowMin)
            rowMin = this.distanceMatrix[i][j];
        }
        for (int j = 0; j < this.box_count; j++) {
          this.distanceMatrix[i][j] -= rowMin;
        }
      }

      //Step 2: Subtract column minimums
      for (int j = 0; j < this.box_count; j++) {
        colMin = Integer.MAX_VALUE; //it's OVER 9000!!!
        for (int i = 0; i < this.box_count; i++) {
          if (this.distanceMatrix[i][j] < colMin)
            colMin = this.distanceMatrix[i][j];
        }
        for (int i = 0; i < this.box_count; i++) {
          this.distanceMatrix[i][j] -= colMin;
        }
      }
    }

    //greedy assignment of "square zeros" (the first zero marked in respect to column and row)
    private void Step3_greedySquares() {
      //temporary matrices for checking if the row has a square
      int[] RowSquaresCheck = new int[box_count];
      int[] ColSquaresCheck = new int[box_count];

      for (int i = 0; i < box_count; i++) {
        for (int j = 0; j < box_count; j++) {
          //if greedy approach to assigning squares
          if (distanceMatrix[i][j] == 0 && ColSquaresCheck[j] == 0 && RowSquaresCheck[i] == 0) {
            RowSquaresCheck[i] = 1;
            ColSquaresCheck[j] = 1;
            RowSquares[i] = j; // we store the square column index for the current row 
            ColSquares[j] = i; // same story for currnet col
            continue;
          }
        }
      }
    }

    //mark put a line on the 'squared' zeroes' columns
    private void Step4_LineMarks() {
      for (int j = 0; j < this.box_count; j++) {
        this.zl_col[j] = ColSquares[j] != -1 ? 1 : 0;
        // 1 means we put a line on the column, 0 means we leave it open for unmarked values
      }
    }

    private int[] Step5_FindMainZero() {
       for (int i = 0; i < box_count; i++) {
        if (this.zl_row[i] == 0) // account only if the row is not covered
          for (int j = 0; j < box_count; j++) {
            if (this.distanceMatrix[i][j] == 0 && this.zl_col[j] == 0) {
              this.RowStars[i] = j; //mark the column of the main zero in the row
              return new int[]{i,j}; // return the main zero position of current row
            }
          }
      }
      return null; // if no main zero found
    }

    private void Step7_StarAndSquareChain(int[] mainZero) {
      int i = mainZero[0];
      int j = mainZero[1];

      Set<int[]> Star_Square_Chain = new LinkedHashSet<>();
      Star_Square_Chain.add(mainZero);
      boolean foundSquare_thenStar = true;

      do {
        if (ColSquares[j] != -1) {
          Star_Square_Chain.add(new int[]{ColSquares[j], j});
          foundSquare_thenStar = true;

        } else {
          foundSquare_thenStar = false;
        }

        if (!foundSquare_thenStar) {
          break;
        }

        //find star in the row of the square found
        //THIS IS THE CHAINING PART!!! (VV IMPORTANT!!)
        i = ColSquares[j];
        j = RowStars[i];

        if (j != -1) { //meaning if there is no star in the column at row i
          Star_Square_Chain.add(new int[]{i,j});
          foundSquare_thenStar = true;
        } else {
          foundSquare_thenStar = false;
        }
      } while (foundSquare_thenStar);

      for (int[] Zero : Star_Square_Chain) {
        if (ColSquares[Zero[1]] == Zero[0]) {
          ColSquares[Zero[1]] = -1;
          RowSquares[Zero[0]] = -1;
        }

        if (RowStars[Zero[0]] == Zero[1]) {
          ColSquares[Zero[1]] = Zero[0];
          RowSquares[Zero[0]] = Zero[1];
        }
      }

      Arrays.fill(this.RowStars, -1);
      Arrays.fill(this.zl_row, 0);
      Arrays.fill(this.zl_col, 0);
    }

    private boolean ColumnsAreAllCovered() {
      for (int i = 0; i < this.box_count; i++) {
        if (this.zl_col[i] == 0) return false;
      }
      return true;
    }

    private void Step8_AddandSubtract() {
      int minUncovered = Integer.MAX_VALUE;
      for (int i = 0; i < this.box_count; i++) {
        if (this.zl_row[i] == 1) continue;
        for (int j = 0; j < this.box_count; j++) {
          if (this.zl_col[j] == 0 && this.distanceMatrix[i][j] < minUncovered)
            minUncovered = this.distanceMatrix[i][j];
        }
      }

      if (minUncovered > 0) {
        for (int i = 0; i < this.box_count; i++) {
          for (int j = 0; j < this.box_count; j++) {
            //add intersections of lines
            if (this.zl_row[i] == 1 && this.zl_col[j] == 1)
              this.distanceMatrix[i][j] += minUncovered;
            //subtract uncovered values
            else if (this.zl_row[i] == 0 && this.zl_col[j] == 0)
              this.distanceMatrix[i][j] -= minUncovered;
          }
        }
      }
    }
  }  // end of HungarianAssignment class

}
