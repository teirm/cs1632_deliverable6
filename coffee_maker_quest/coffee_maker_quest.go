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
const delim = '\n' //For reading only single lines

func adder(x int, y int) int {
	return x + y
}

func process_bag(bag_status int) int {

	if bag_status == 7 {
		return 1
	} else if bag_status == 0 {
		return 0
	} else {
		return -1
	}
}

func display_instructions() bool {

	return true

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

	/* Create current Player */
	current_player := Player{0, 0, 0, 1}
	fmt.Println("Player is: ", current_player)

	r := bufio.NewReader(os.Stdin)
	fmt.Printf("Coffee Maker Quest 2.0\n")

	r.ReadString(delim)

	for current_player.keep_going == 1 {
		
	
	
	}
}
