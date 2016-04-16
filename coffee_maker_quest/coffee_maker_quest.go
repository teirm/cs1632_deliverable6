package coffee_maker_quest

import (
	"bufio"
	"fmt"
	"os"
)

/* Structs */
type Room struct {
	north_door string
	south_door string
	furniture  string
	item       string
	room_adj   string
}

type Player struct {
	current_pos int
	win_status  int
	bag_status  int
	keep_going  int
}

/* Constants */
const delim = '\n'     //For reading only single lines
const total_states = 6 //Total number of rooms in game

/* DUMMY FUNCTION */
func adder(x int, y int) int {
	return x + y
}

/* MOVES THE PLAYER ONE ROOM NORTH (IF POSSIBRU) */
func move_north(cur_pos int, total_states int) int{
	return 0;
}

/* PROCESSES BAG TO DETERMINE WIN/LOSS */
func process_bag(bag_status int) int {

	if bag_status == 7 {
		return 1
	} else if bag_status == 0 {
		return 0
	} else {
		return -1
	}
}

/* DISPLAYS INSTRUCTIONS */
func display_instructions() bool {
	fmt.Printf("\n INSTRUCTIONS (N, S, L, I, H, D) >\n")
	return true
}

/* INITIALIZE GAME */
func init_game(states int, array []Room) {

	for i := 0; i < states; i++ {
		array[i] = Room{"S","K","K","L","M"}
	}
}

/* Example of user Input
r := bufio.NewReader(os.Stdin)
fmt.Printf("Enter a string: ")
line, err := r.ReadString(delim)
if err != nil {
	fmt.Println(err)
	os.Exit(1)
}
fmt.Printf(line) */

func Run() {
	/* Create a reader */
	r := bufio.NewReader(os.Stdin)

	/* Create current Player */
	current_player := Player{0, 0, 0, 1}

	/* Create array of rooms for traversal */
	var game_states [total_states]Room
	init_game(total_states, game_states[:])

	fmt.Println("Player is: ", current_player)
	fmt.Printf("Coffee Maker Quest 2.0\n")

	for current_player.keep_going == 1 {
		display_instructions()
		r.ReadString(delim)
	}
}
