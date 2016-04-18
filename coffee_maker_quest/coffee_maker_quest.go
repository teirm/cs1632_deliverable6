package coffee_maker_quest

import (
	"bufio"
	"fmt"
	"os"
	"strings"
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
	room_num     int
	win_status   int
	coffee_items int
	keep_going   int
}

/* Constants */
const delim = '\n'        //For reading only single lines
const total_states = 6    //Total number of rooms in game
const inventory_slots = 3 //Total number of inventory slots

/* MOVES THE PLAYER ONE ROOM NORTH (IF POSSIBRU) */
func move_north(cur_pos int, total_states int) int {
	next_pos := cur_pos + 1

	if next_pos < total_states {
		return next_pos
	} else {
		return cur_pos
	}
}

/* MOVES THE PLAYER ONE ROOM SOUTH (IF POSSIBRU) */
func move_south(cur_pos int, total_states int) int {
	next_pos := cur_pos - 1

	if next_pos >= 0 {
		return next_pos
	} else {
		return cur_pos
	}
}

/* PROCESSES BAG TO DETERMINE WIN/LOSS */
func process_bag(coffee_items int) int {

	if coffee_items == 7 {
		return 1
	} else if coffee_items == 0 {
		return 0
	} else {
		return -1
	}
}

/* DISPLAYS INSTRUCTIONS */
func display_commands() bool {
	fmt.Printf("\n INSTRUCTIONS (N, S, L, I, H, D) >\n")
	return true
}

/* INITIALIZE GAME */
func init_game(array []Room) {
	fmt.Printf("Coffee Maker Quest 2.0 (Golang Edition)\n")

	for i := 0; i < len(array); i++ {
		create_room(i, &array[i])
	}
}

/* CREATE ROOMS */
func create_room(room_pos int, room *Room) int {

	adj_furn_array := [...]string{"Small", "Quaint sofa", "Magenta", "NONE",
		"Funny", "Sad record player", "Beige", "Massive",
		"Refinanced", "Tight pizza", "Dead", "Smart",
		"Dumb", "Flat energy drink", "Vivacious", "Slim",
		"Bloodthirsty", "Beautiful bag of money", "Purple", "Sandy",
		"Rough", "Perfect air hockey table", "NONE", "Minimalist"}

	index := room_pos * 4

	if room_pos == 0 {
		room.room_adj = adj_furn_array[index+0]
		room.furniture = adj_furn_array[index+1]
		room.north_door = adj_furn_array[index+2]
		room.item = "Cream"
	} else if room_pos == total_states/2 {
		room.room_adj = adj_furn_array[index+0]
		room.furniture = adj_furn_array[index+1]
		room.north_door = adj_furn_array[index+2]
		room.south_door = adj_furn_array[index+3]
		room.item = "Coffee"
	} else if room_pos == total_states-1 {
		room.room_adj = adj_furn_array[index+0]
		room.furniture = adj_furn_array[index+1]
		room.south_door = adj_furn_array[index+3]
		room.item = "Sugar"
	} else {
		room.room_adj = adj_furn_array[index+0]
		room.furniture = adj_furn_array[index+1]
		room.north_door = adj_furn_array[index+2]
		room.south_door = adj_furn_array[index+3]
	}

	return 1
}

/* CHECK USER INPUT */
func check_input(in string) bool {

	out := true

	if !strings.EqualFold(in, "N") && !strings.EqualFold(in, "S") &&
		!strings.EqualFold(in, "L") && !strings.EqualFold(in, "I") &&
		!strings.EqualFold(in, "H") && !strings.EqualFold(in, "D") {
		out = false
	}

	return out

}

/* DISPLAY INSTRUCTIONS */
func display_instructions() int {

	fmt.Printf("Instructions for Coffee Maker Quest -\n" +
		"You are a brave student trying to study for finals, but you need caffeine.\n" +
		"The goal of the game is to collect sugar, coffee, and cream so that you can study.\n")

	return 1
}

/* DISPLAY INVENTORY */
func display_inventory(inventory []string) int {

	has_coffee := 0x000
	has_cream := 0x000
	has_sugar := 0x000

	for i := 0; i < len(inventory); i++ {
		if strings.EqualFold(inventory[i], "Coffee") {
			has_coffee = 1
			fmt.Printf("You have a cup of delicious coffee.\n")
			break
		}
	}

	if has_coffee == 0 {
		fmt.Printf("YOU HAVE NO COFFEE!\n")
	}

	for i := 0; i < len(inventory); i++ {
		if strings.EqualFold(inventory[i], "Cream") {
			has_cream = 2
			fmt.Printf("You have some fresh cream.\n")
			break
		}
	}

	if has_cream == 0 {
		fmt.Printf("YOU HAVE NO CREAM!\n")
	}

	for i := 0; i < len(inventory); i++ {
		if strings.EqualFold(inventory[i], "Sugar") {
			has_sugar = 4
			fmt.Printf("You have some fresh sugar.\n")
		}
	}

	if has_sugar == 0 {
		fmt.Printf("YOU HAVE NO SUGAR!\n")
	}

	return has_coffee + has_cream + has_sugar
}

/* DISPLAY ROOM DESCRIPTION */
func display_room(room Room) string {
	// what kind of room
	var description string = "\nYou see a " + room.room_adj + " room.\n"

	// what kind of item in the room
	description += "It has a " + room.furniture + ".\n"

	// door leading north (if any)
	if room.north_door != "" {
		description += "A " + room.north_door + " door leads North.\n"
	}

	// door leading south (if any)
	if room.south_door != "" {
		description += "A " + room.south_door + " door leads South.\n"
	}

	return description
}

/* LOOK IN ROOM AND RETURN COFFEE ITEM (IF EXISTS) */
func search_room_for_coffee_items(room Room, inventory []string) string {
	fmt.Println("Searching room")

	switch room.item {
	case "Coffee", "Cream", "Sugar":
		{
			fmt.Println("There might be something here...")
			fmt.Println("You found some", room.item)

			// add to inventory
			if room.item == "Coffee" {
				inventory[0] = room.item
			} else if room.item == "Cream" {
				inventory[1] = room.item
			} else {
				inventory[2] = room.item
			}

			return room.item
		}
	default:
		{
			fmt.Println("You don't see anything out of the ordinary.")
		}
	}

	return ""
}

/* DRINK; DETERMINE IF WIN OR LOSE */
func drink(current_player Player, inventory []string) int {
	current_player.coffee_items = display_inventory(inventory[:])
	status := process_bag(current_player.coffee_items)

	if status == 1 {
		fmt.Println("\nYou Win!")
	} else {
		fmt.Println("\nYou Lose!")
	}

	return status
}

func play(current_player Player, rooms []Room, inventory []string, user_input string) Player {
	// figure out what to do
	if check_input(user_input) == false {
		fmt.Println("What?")

	} else {
		switch user_input {
		case "N": // MOVE NORTH
			{
				current_player.room_num = move_north(current_player.room_num, total_states)
			}
		case "S": // MOVE SOUTH
			{
				current_player.room_num = move_south(current_player.room_num, total_states)
			}
		case "L": // LOOK
			{
				search_room_for_coffee_items(rooms[current_player.room_num], inventory[:])
			}
		case "I": // CHECK INVENTORY
			{
				current_player.coffee_items = display_inventory(inventory[:])
			}
		case "H": // DISPLAY HELP
			{
				display_instructions()
			}
		case "D": // DRINK
			{
				current_player.win_status = drink(current_player, inventory[:])

				current_player.keep_going = 0 // stop
			}
		}
	}

	return current_player
}

func Run() {
	/* Create a reader for user input*/
	r := bufio.NewReader(os.Stdin)

	/* Create current Player */
	current_player := Player{0, 0, 0, 1}

	/* Create array of rooms for traversal */
	var rooms [total_states]Room
	init_game(rooms[:])

	// the game needs an inventory of what the player has collected
	var inventory [inventory_slots]string

	for current_player.keep_going == 1 {
		// always display the room
		fmt.Printf(display_room(rooms[current_player.room_num]))

		// always display the list of commands
		display_commands()

		// get user input (normalized)
		user_input, _ := r.ReadString(delim)
		user_input = strings.TrimSpace(strings.ToUpper(user_input))

		// figure out what to do
		current_player = play(current_player, rooms[:], inventory[:], user_input)
	}
}
