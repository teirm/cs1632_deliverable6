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

func Adder(x int, y int) int {
	return x + y
}

func Run() {
	fmt.Printf("Kittens\n")
}
