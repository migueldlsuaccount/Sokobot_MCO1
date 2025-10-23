package solver;

import java.util.*;

public class SokoBot {

    // mapData contains walls (#) and goal boxes (.)
    // itemsData contains boxes ($) and player (@)

    // so we need positions of boxes, player, and goal boxes... will also be used later for heuristic calculation
    private int width, height;
    private ArrayList<int[]> Position_boxes = new ArrayList<>(); //will be modified during decoding of states
    private HashSet<String> boxMap = new HashSet<>(); //for linear time checking of boxes and collisions quickly
    private ArrayList<int[]> Position_goals = new ArrayList<>();
    private int[] Position_player = new int[2]; //also to be modified during decoding of states
    private char[][] WallsMap; //for checking walls
    //auxillaries
    private HashSet<String> Position_deadlocks = new HashSet<>(); //if box is in any of these positions, do not add to frontier aka prune
    private int box_count = 0; // this will be used for key in hashmap when decoding positions
    private int[][] box_assignments; //box to goal assignments through self-made Hungarian algo

    private HashMap<String, State> ExploredStates = new HashMap<>();

    //Stumbled upon PriorityQueue, where we could store Frontiers in order based on a value that is being compareTo()'d by the class implementeing Comparator:
    //https://www.geeksforgeeks.org/java/priority-queue-in-java/
    private PriorityQueue<State> Frontier;
    private char[] validMoves;
    
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

    //make sure to update this.box_positions when decoding states for heuristic computation
    
    this.Frontier = new PriorityQueue<>();

    //establish THE VERY FIRSTTTT.... state.

    String InitialStateKey = EncodeState(this.Position_player, this.Position_boxes);
    State initialState = new State(InitialStateKey, null, 0, computeManhattanDistances(), '1');
    Frontier.add(initialState);

    // A* babyyyyy
    while(!Frontier.isEmpty()) 
    {
      //get the current state with lowest f_cost
      State currentState = Frontier.poll();

      if (this.ExploredStates.containsKey(currentState.getStateKey())) {
        continue; // state already explored
      }

      //updates Position_boxes, Position_player, boxMap
      DecodeState(currentState.getStateKey());

      //TO BE FILLED LATER
      if (State_isA_GoalState()) {
        return PathToSolution(currentState);
      }

      ExploredStates.put(currentState.getStateKey(), currentState);

      //exploration!!!: note current positions are from decoded current state explored
      ExploreNeighborStates(currentState.getStateKey(), currentState.getG_Cost());
    }

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

  //to be filled laterr
  //we currently have current state positions
  //our format is "uldrluludur"
  public String PathToSolution(State goalState) {
    StringBuilder solutionPath = new StringBuilder();
    while ((goalState.getPrevMove() != '1') && (goalState.getPreviousStateKey() != null)) {
      solutionPath.insert(0, goalState.getPrevMove());
      goalState = this.ExploredStates.get(goalState.getPreviousStateKey());
    }
    return solutionPath.toString(); //temporary
  }

  public void ExploreNeighborStates(String prevStateKey, int g_cost) {
    for (char move : this.validMoves) {
      //check first if move is valid before generating new state
      if (Valid_Move(move)) {
        //idea, include checking of state in Valid_Move if state is explored
        //if valid, update positions accordingly
        UpdatePositions(move);
        
        int h_cost = computeHeuristic();

        //we can add penalties here
        //h_cost += penaltyfunction();

        State newState = new State(
          EncodeState(this.Position_player, this.Position_boxes),
          prevStateKey, g_cost + 1, g_cost + 1 + h_cost, move);

        this.Frontier.add(newState);

        DecodeState(prevStateKey); //revert back to previous state for next move exploration
      }
    }
  }

  //current version for faster computation
  public int computeHeuristic() {
    return computeManhattanDistances();
  }

  //this is the first version of computeHeuristic, very expensive
  // public int computeHeuristic() {
  //   HungarianAssignment box_assignment_HA = new HungarianAssignment(this.box_count);
  //   this.box_assignments = box_assignment_HA.HungarianAlgorithm();
  //   return computeManhattanDistances();
  // }

  public void UpdatePositions(char move) {
    switch(move) {
      case 'u':
        if (this.boxMap.contains((this.Position_player[0]-1) + "," + this.Position_player[1])) {
          //moving box
          this.boxMap.add((this.Position_player[0]-2) + "," + this.Position_player[1]);
          this.boxMap.remove((this.Position_player[0]-1) + "," + this.Position_player[1]);

          //also update Position_boxes
          for (int i = 0; i < this.box_count; i++) {
            if ((this.Position_boxes.get(i)[0] == this.Position_player[0]-1) &&
                (this.Position_boxes.get(i)[1] == this.Position_player[1])) {
              this.Position_boxes.get(i)[0] = this.Position_player[0]-2;
              break;
            }
          }
        }
        this.Position_player[0]--;
      break;
      case 'd':
        if (this.boxMap.contains((this.Position_player[0]+1) + "," + this.Position_player[1])) {
          //moving box
          this.boxMap.add((this.Position_player[0]+2) + "," + this.Position_player[1]);
          this.boxMap.remove((this.Position_player[0]+1) + "," + this.Position_player[1]);

          //also update Position_boxes
          for (int i = 0; i < this.box_count; i++) {
            if ((this.Position_boxes.get(i)[0] == this.Position_player[0]+1) &&
                (this.Position_boxes.get(i)[1] == this.Position_player[1])) {
              this.Position_boxes.get(i)[0] = this.Position_player[0]+2;
              break;
            }
          }
        }
        this.Position_player[0]++;
      break;
      case 'l':
        if (this.boxMap.contains(this.Position_player[0] + "," + (this.Position_player[1]-1))) {
          //moving box
          this.boxMap.add(this.Position_player[0] + "," + (this.Position_player[1]-2));
          this.boxMap.remove(this.Position_player[0] + "," + (this.Position_player[1]-1));

          //also update Position_boxes
          for (int i = 0; i < this.box_count; i++) {
            if ((this.Position_boxes.get(i)[0] == this.Position_player[0]) &&
                (this.Position_boxes.get(i)[1] == this.Position_player[1]-1)) {
              this.Position_boxes.get(i)[1] = this.Position_player[1]-2;
              break;
            }
          }
        }
        this.Position_player[1]--;
      break;
      case 'r':
        if (this.boxMap.contains(this.Position_player[0] + "," + (this.Position_player[1]+1))) {
          //moving box
          this.boxMap.add(this.Position_player[0] + "," + (this.Position_player[1]+2));
          this.boxMap.remove(this.Position_player[0] + "," + (this.Position_player[1]+1));

          //also update Position_boxes
          for (int i = 0; i < this.box_count; i++) {
            if ((this.Position_boxes.get(i)[0] == this.Position_player[0]) &&
                (this.Position_boxes.get(i)[1] == this.Position_player[1]+1)) {
              this.Position_boxes.get(i)[1] = this.Position_player[1]+2;
              break;
            }
          }
        }
        this.Position_player[1]++;
      break;
    }
  }

  
  public boolean Valid_Move (char move) {
    //switch case for the moves
    //obtain player position
    // if player collides with box, check if box can be moved (change box position accordingly)
    // if player collides with wall, invalid
    //should also encode the positions to a statekey to check if explored
    String stateKey_temp;
    int[] player_pos_temp = new int[] {this.Position_player[0], this.Position_player[1]};
    switch(move) {
      case 'u':
        if (this.WallsMap[this.Position_player[0]-1][this.Position_player[1]] == '#') {
          return false;
        }
        else if (this.WallsMap[this.Position_player[0]-1][this.Position_player[1]] == ' ' &&
            !this.boxMap.contains((this.Position_player[0]-1) + "," + this.Position_player[1])) {
          player_pos_temp = new int[] {this.Position_player[0]-1, this.Position_player[1]};
          stateKey_temp = EncodeState(player_pos_temp, this.Position_boxes);
          if (!isAlreadyExplored(stateKey_temp)) { 
            return true;
          }
          else {
            return false;
          }
        }
        else if (!Player_pushcollidingbox_valid('u')) {
          return false;
        }
        break;
      case 'd':
        if (this.WallsMap[this.Position_player[0]+1][this.Position_player[1]] == '#') {
          return false;
        }
        else if (this.WallsMap[this.Position_player[0]+1][this.Position_player[1]] == ' ' &&
            !this.boxMap.contains((this.Position_player[0]+1) + "," + this.Position_player[1])) {
          player_pos_temp = new int[] {this.Position_player[0]+1, this.Position_player[1]};
          stateKey_temp = EncodeState(player_pos_temp, this.Position_boxes);
          if (!isAlreadyExplored(stateKey_temp)) { 
            return true;
          }
          else {
            return false;
          }
        }
        else if (!Player_pushcollidingbox_valid('d')) {
          return false;
        }
      break;
      case 'l':
        if (this.WallsMap[this.Position_player[0]][this.Position_player[1]-1] == '#') {
          return false;
        }
        else if (this.WallsMap[this.Position_player[0]][this.Position_player[1]-1] == ' ' &&
            !this.boxMap.contains(this.Position_player[0] + "," + (this.Position_player[1]-1))) {
          player_pos_temp = new int[] {this.Position_player[0], this.Position_player[1]-1};
          stateKey_temp = EncodeState(player_pos_temp, this.Position_boxes);
          if (!isAlreadyExplored(stateKey_temp)) { 
            return true;
          }
          else {
            return false;
          }
        }
        else if (!Player_pushcollidingbox_valid('l')) {
          return false;
        }
      break;
      case 'r':
        if (this.WallsMap[this.Position_player[0]][this.Position_player[1]+1] == '#') {
          return false;
        }
        else if (this.WallsMap[this.Position_player[0]][this.Position_player[1]+1] == ' ' &&
            !this.boxMap.contains(this.Position_player[0] + "," + (this.Position_player[1]+1))) {
          player_pos_temp = new int[] {this.Position_player[0], this.Position_player[1]+1};
          stateKey_temp = EncodeState(player_pos_temp, this.Position_boxes);
          if (!isAlreadyExplored(stateKey_temp)) { 
            return true;
          }
          else {
            return false;
          }
        }
        else if (!Player_pushcollidingbox_valid('r')) {
          return false;
        }
      break;
    }
    return true;
  }

  
  public boolean isAlreadyExplored(String stateKey) {
    return this.ExploredStates.containsKey(stateKey);
  }

  //outputs false if push is invalid or state is explored
  public boolean Player_pushcollidingbox_valid(char move) {
    String box_pos1, box_pos2;
    // cannot do ArrayList<int[]> temp_box_positions = this.Position_boxes; reference only kasi, hard copy instead
    int[] new_player_pos_temp;
    ArrayList<int[]> temp_box_positions;
    switch(move) {
      case 'u':
        box_pos1 = (this.Position_player[0]-1) + "," + this.Position_player[1];
        box_pos2 = (this.Position_player[0]-2) + "," + this.Position_player[1];
        
        //three invalid scenarios: player,box,wall ; player,box,box ; player,box -> deadlock
        if (this.boxMap.contains(box_pos1) && 
          ((this.WallsMap[this.Position_player[0]-2][this.Position_player[1]] == '#') || 
          this.boxMap.contains(box_pos2) || 
          this.Position_deadlocks.contains(box_pos2))) {
          return false;
        }
        
        else if (this.boxMap.contains(box_pos1)) {
          new_player_pos_temp = new int[] {this.Position_player[0]-1, this.Position_player[1]};
          temp_box_positions = CreateTemp_BoxPositions(
            new int[]{this.Position_player[0]-1, this.Position_player[1]}, 
            new int[]{this.Position_player[0]-2, this.Position_player[1]});
          String statekey_temp = EncodeState(new_player_pos_temp, temp_box_positions);
          
          return !isAlreadyExplored(statekey_temp);
        }
      break;
      case 'd':
        box_pos1 = (this.Position_player[0]+1) + "," + this.Position_player[1];
        box_pos2 = (this.Position_player[0]+2) + "," + this.Position_player[1];
        
        //three invalid scenarios: player,box,wall ; player,box,box ; player,box -> deadlock
        if (this.boxMap.contains(box_pos1) && 
          ((this.WallsMap[this.Position_player[0]+2][this.Position_player[1]] == '#') || 
          this.boxMap.contains(box_pos2) || 
          this.Position_deadlocks.contains(box_pos2))) {
          return false;
        }

        else if (this.boxMap.contains(box_pos1)) {
          new_player_pos_temp = new int[] {this.Position_player[0]+1, this.Position_player[1]};
          temp_box_positions = CreateTemp_BoxPositions(
            new int[]{this.Position_player[0]+1, this.Position_player[1]}, 
            new int[]{this.Position_player[0]+2, this.Position_player[1]});
          String statekey_temp = EncodeState(new_player_pos_temp, temp_box_positions);
          
          return !isAlreadyExplored(statekey_temp);
        }
      break;
      case 'l':
        box_pos1 = this.Position_player[0] + "," + (this.Position_player[1]-1);
        box_pos2 = this.Position_player[0] + "," + (this.Position_player[1]-2);
        
        //three invalid scenarios: player,box,wall ; player,box,box ; player,box -> deadlock
        if (this.boxMap.contains(box_pos1) && 
          ((this.WallsMap[this.Position_player[0]][this.Position_player[1]-2] == '#') || 
          this.boxMap.contains(box_pos2) || 
          this.Position_deadlocks.contains(box_pos2))) {
          return false;
        }

        else if (this.boxMap.contains(box_pos1)) {
          new_player_pos_temp = new int[] {this.Position_player[0], this.Position_player[1]-1};
          temp_box_positions = CreateTemp_BoxPositions(
            new int[]{this.Position_player[0], this.Position_player[1]-1}, 
            new int[]{this.Position_player[0], this.Position_player[1]-2});
          String statekey_temp = EncodeState(new_player_pos_temp, temp_box_positions);
          
          return !isAlreadyExplored(statekey_temp);
        }
      break;
      case 'r':
        box_pos1 = this.Position_player[0] + "," + (this.Position_player[1]+1);
        box_pos2 = this.Position_player[0] + "," + (this.Position_player[1]+2);
        
        //three invalid scenarios: player,box,wall ; player,box,box ; player,box -> deadlock
        if (this.boxMap.contains(box_pos1) && 
          ((this.WallsMap[this.Position_player[0]][this.Position_player[1]+2] == '#') || 
          this.boxMap.contains(box_pos2) || 
          this.Position_deadlocks.contains(box_pos2))) {
          return false;
        }
        else if (this.boxMap.contains(box_pos1)) {
          new_player_pos_temp = new int[] {this.Position_player[0], this.Position_player[1]+1};
          temp_box_positions = CreateTemp_BoxPositions(
            new int[]{this.Position_player[0], this.Position_player[1]+1}, 
            new int[]{this.Position_player[0], this.Position_player[1]+2});
          String statekey_temp = EncodeState(new_player_pos_temp, temp_box_positions);
          
          return !isAlreadyExplored(statekey_temp);
        }
      break;
    }
    return true;
  }

  public ArrayList<int[]> CreateTemp_BoxPositions(int[] old_box_pos, int[] new_box_pos) {
    ArrayList<int[]> temp_box_pos = new ArrayList<>();
    for (int i = 0; i < this.box_count; i++) {
      if (this.Position_boxes.get(i)[0] == old_box_pos[0] &&
          this.Position_boxes.get(i)[1] == old_box_pos[1]) {
            temp_box_pos.add(new int[] {new_box_pos[0], new_box_pos[1]});
          }
          else {
            temp_box_pos.add(new int[] {this.Position_boxes.get(i)[0], this.Position_boxes.get(i)[1]});
          }
    }
    return temp_box_pos;
  }
  /*we should have the following functions:
    1. ValidMove
    2. UpdatePositions
    3. IsGoalState

  */

  //Initially I was thinking of doing manhattan distance of each box to every goal, it wasn't efficient, and I thought rin na choosing the min manhattahn idstance for each box to their closest goal is not enough (too much repeated computation). I came across Hungarian algorithm which reduces the computation needed for not needing the repeated finding of minimum distances. I implemented box to goal assigned manhattan distances (that were assigned via Hungarian algo)
  //serves as heuristic function
  // ONLY COMPUTES THE MANHATTAN DISTANCE OF BOXES TO ASSIGNED GOALS, NO PENALTY YET FOR DEADLOCKS
  // IMPORTANT!!!: CALL AFTER ASSIGNING BOXES TO GOALS
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

  //just search how to make string out of arrays and I eventually found this:
  //https://www.geeksforgeeks.org/java/stringbuilder-class-in-java-with-examples/
  public String EncodeState(int[] player, ArrayList<int[]> boxes) {
    StringBuilder StateKey_enc = new StringBuilder();
    StateKey_enc.append(player[0]).append(',').append(player[1]);
    for (int[] box : boxes) {
      StateKey_enc.append(' ').append(box[0]).append(',').append(box[1]);
    }
    return StateKey_enc.toString();
  }

  private boolean State_isA_GoalState() {
    int min_distance_temp;
    int temp_dist;
    for (int i = 0; i < this.box_count; i++) {
      min_distance_temp = Integer.MAX_VALUE;
      for(int j = 0; j < this.box_count; j++) {
        temp_dist = computeManhattanDistance(this.Position_boxes.get(i), this.Position_goals.get(j));
        if (temp_dist < min_distance_temp) {
          min_distance_temp = temp_dist;
        }
      }
      if (min_distance_temp > 0) {
        return false;
      }
    }
    return true;
  }

  public void DecodeState(String stateKey_dec) {
    String[] positions = stateKey_dec.split(" ");
    String[] player_pos = positions[0].split(",");
    //we are trying to modify the SokoBot class variables here
    this.Position_player[0] = Integer.parseInt(player_pos[0]);
    this.Position_player[1] = Integer.parseInt(player_pos[1]);
    this.Position_boxes.clear();
    this.boxMap.clear();
    //now update Position_boxes
    for (int i = 1; i < positions.length; i++) {
      this.boxMap.add(positions[i]);
      String[] box_pos = positions[i].split(",");
      this.Position_boxes.add(new int[]{Integer.parseInt(box_pos[0]), Integer.parseInt(box_pos[1])});
    }
    //maybe we should make a map of the boxes for easier checking of valid move, FIXED, added this.boxMap.add(positions[i]);
  }

  //class to represent position
  private class State implements Comparable<State> {
    //String stateKey; //stateKey will be passed down through functions naman, myb no need
    String StateKey;
    String previousStateKey;
    int g_Cost;
    int f_cost;
    //the move that led to the state from previous state
    char prevMove; //'u', 'd', 'l', 'r'

    public State (String stateKey, String previousStateKey, int g_Cost, int f_cost, char previousMove) {
      this.StateKey = stateKey;
      this.previousStateKey = previousStateKey;
      this.g_Cost = g_Cost;
      this.f_cost = f_cost;
      this.prevMove = previousMove;
    } 

    public String getStateKey() {
      return this.StateKey;
    }

    public String getPreviousStateKey() {
      return this.previousStateKey;
    }

    public char getPrevMove() {
      return this.prevMove;
    }

    public int getG_Cost() {
      return this.g_Cost;
    }

    public int getF_Cost() {
      return this.f_cost;
    }

    //by default this has to be overriden for classes that implement Comparable
    @Override
    public int compareTo(State other_state) {
      return Integer.compare(this.f_cost, other_state.getF_Cost());
    }
  }


  private void initializeMapData(int width, int height, char[][] mapData, char[][] itemsData) {
    this.validMoves = new char[] {'u', 'd', 'l', 'r'};
    this.width = width;
    this.height = height;
    
    //we copy mapdata to WallsMap but goals are spaces
    this.WallsMap = new char[height][width];
    for (int i = 0; i < height; i++) {
      for (int j = 0; j < width; j++) {
        this.WallsMap[i][j] = (mapData[i][j] == '.') ? ' ' : mapData[i][j];
      }
    }

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
              Position_deadlocks.add(i + "," + j);
            //upper diagonal right
            else if (mapData[i-1][j+1] == '#' && mapData[i][j+1] == '#')
              Position_deadlocks.add(i + "," + j);
          }
          //wall below
          else if (mapData[i+1][j] == '#') {
            //lower diagonal left
            if (mapData[i+1][j-1] == '#' && mapData[i][j-1] == '#')
              Position_deadlocks.add(i + "," + j);
            //lower diagonal right
            else if (mapData[i+1][j+1] == '#' && mapData[i][j+1] == '#')
              Position_deadlocks.add(i + "," + j);
          }
        }
        if (mapData[i][j] == '#') this.WallsMap[i][j] = '#';
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
