package coffee_maker_quest

import (
	"fmt"
)

/* Structs */
type room struct {
	north_door string
	south_door string
	furniture  string
	item       string
	room_adj   string
}

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

func Run() {
	fmt.Printf("Kittens\n")
}
