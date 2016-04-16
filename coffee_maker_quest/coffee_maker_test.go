/*Process for committing:
  Commit just test + min to compile
  Commit green
  Commit Refactor
*/

/* Name of Directory containing file */
package coffee_maker_quest

/* Testing imports */
import (
	. "github.com/onsi/ginkgo"
	. "github.com/onsi/gomega"
	"strings"
	"testing"
)

/* Test method names have to start with 'Test'
Different types of tests exist: *testing.<Type>
See https://golang.org/pkg/testing/ for more info */

func Test_example(t *testing.T) {
	RegisterFailHandler(Fail)
	RunSpecs(t, "Coffee Maker Quest")
}

/* Process Bag Tests */
func Test_empty_bag(t *testing.T) {

	exp := 0
	act := process_bag(0)

	if exp != act {
		t.Fatalf("Expected %d got %d", exp, act)
	}
}

func Test_full_bag(t *testing.T) {

	exp := 1
	act := process_bag(7)
	if exp != act {
		t.Fatalf("Expected %d got %d", exp, act)
	}
}

func Test_negative_bag(t *testing.T) {

	exp := -1
	act := process_bag(-1)
	if exp != act {
		t.Fatalf("Expected %d got %d", exp, act)
	}
}

func Test_over_sized_bag(t *testing.T) {

	exp := -1
	act := process_bag(30)
	if exp != act {
		t.Fatalf("Expected %d got %d", exp, act)
	}
}

/* Display Instructions Tests */
func Test_display_commands(t *testing.T) {

	exp := true
	act := display_commands()

	if exp != act {
		t.Fatalf("Expected %d got %d", exp, act)
	}
}

/* Move North Test */
func Test_move_north(t *testing.T) {

	// consider a test room struct that
	//		contains 5 rooms
	total_rooms := 5
	cur_room := 0 // zero-based
	exp := 1

	// try to move from room 0 to 1
	act := move_north(cur_room, total_rooms)

	if exp != act {
		t.Fatalf("Expected %d room, got %d", exp, act)
	}
}

/* Initialization Tests */
func Test_valid_state_count(t *testing.T) {

	const array_states = 4
	exp := 4

	var test_states [array_states]Room
	init_game(test_states[:])

	act := len(test_states)

	if exp != act {
		t.Fatalf("Expected %d got %d", exp, act)
	}
}

func Test_state_correctness(t *testing.T) {

	const array_states = 6
	var test_states [array_states]Room

	test_room := 3

	exp_radj := "Dumb"
	exp_furn := "Flat energy drink"
	exp_nd := "Vivacious"
	exp_sd := "Slim"
	exp_item := "Coffee"

	init_game(test_states[:])
	act_nd := test_states[test_room].north_door
	act_sd := test_states[test_room].south_door
	act_furn := test_states[test_room].furniture
	act_radj := test_states[test_room].room_adj
	act_item := test_states[test_room].item

	if !strings.EqualFold(exp_nd, act_nd) {
		t.Fatalf("Expected %s got %s", exp_nd, act_nd)
	}

	if !strings.EqualFold(exp_sd, act_sd) {
		t.Fatalf("Expected %s got %s", exp_sd, act_sd)
	}

	if !strings.EqualFold(exp_furn, act_furn) {
		t.Fatalf("Expected %s got %s", exp_furn, act_furn)
	}

	if !strings.EqualFold(exp_radj, act_radj) {
		t.Fatalf("Expected %s got %s", exp_radj, act_radj)
	}

	if !strings.EqualFold(exp_item, act_item) {
		t.Fatalf("Expected %s got %s", exp_item, act_item)
	}
}

/* Create Room Tests */
func Test_create_room(t *testing.T) {

	room_pos := 2

	var test_room Room

	exp_radj := "Refinanced"
	exp_furn := "Tight pizza"
	exp_nd := "Dead"
	exp_sd := "Smart"
	exp_item := ""

	exp_ret := 1

	/* passes test_room as a pointer */
	act_ret := create_room(room_pos, &test_room)

	act_nd := test_room.north_door
	act_sd := test_room.south_door
	act_furn := test_room.furniture
	act_radj := test_room.room_adj
	act_item := test_room.item

	if !strings.EqualFold(exp_nd, act_nd) {
		t.Fatalf("Expected %s got %s", exp_nd, act_nd)
	}

	if !strings.EqualFold(exp_sd, act_sd) {
		t.Fatalf("Expected %s got %s", exp_sd, act_sd)
	}

	if !strings.EqualFold(exp_furn, act_furn) {
		t.Fatalf("Expected %s got %s", exp_furn, act_furn)
	}

	if !strings.EqualFold(exp_radj, act_radj) {
		t.Fatalf("Expected %s got %s", exp_radj, act_radj)
	}

	if !strings.EqualFold(exp_item, act_item) {
		t.Fatalf("Expected %s got %s", exp_item, act_item)
	}

	if exp_ret != act_ret {
		t.Fatalf("Expected %s got %s", exp_ret, act_ret)
	}
}

func Test_check_room_item(t *testing.T) {

	room_pos := 0
	var test_room Room

	exp_item := "Cream"
	exp_ret := 1

	act_ret := create_room(room_pos, &test_room)

	act_item := test_room.item

	if !strings.EqualFold(exp_item, act_item) {
		t.Fatalf("Expected %s got %s", exp_item, act_item)
	}

	if exp_ret != act_ret {
		t.Fatalf("Expected %s got %s", exp_ret, act_ret)
	}
}

/* INPUT TESTS */

func Test_input_N(t *testing.T) {

	input_1 := check_input("N")
	if input_1 != true {
		t.Fatalf("Exected true got false")
	}
}

func Test_input_D(t *testing.T) {

	input_2 := check_input("D")
	if input_2 != true {
		t.Fatalf("Exected true got false")
	}
}

func Test_invalid_input(t *testing.T) {
	input_3 := check_input("Q")
	if input_3 != false {
		t.Fatalf("Expected false got true")
	}
}

func Test_invalid_string(t *testing.T) {
	input_4 := check_input("Zustandsumme")
	if input_4 != false {
		t.Fatalf("Expected false got true")
	}
}

func Test_first_char_string(t *testing.T) {
	input_5 := check_input("Daenerys")
	if input_5 != false {
		t.Fatalf("Expected false got true")
	}
}

func Test_case_insensitivity(t *testing.T) {
	input_6 := check_input("S")
	input_7 := check_input("s")

	if (input_6 && input_7) != true {
		t.Fatalf("Expected true, true got one false")
	}
}

func Test_invalid_case_insensitivity(t *testing.T) {
	input_8 := check_input("Z")
	input_9 := check_input("z")

	if (input_8 || input_9) != false {
		t.Fatalf("Expected false, false got one true")
	}
}

/* DISPLAY INSTRUCTIONS TEST */
func Test_display_instructions(t *testing.T) {
	exp := 1
	act := display_instructions()

	if exp != act {
		t.Fatalf("Expected %d got %d", exp, act)
	}
}

/* DISPLAY INVENTORY TEST */
func Test_display_empty_inventory(t *testing.T) {

	const inventory_size = 3
	exp := 0

	var inventory [inventory_size]string

	act := display_inventory(inventory[:])

	if exp != act {
		t.Fatalf("Expected %d got %d", exp, act)
	}
}

func Test_wrong_item_inventory(t *testing.T) {

	const inventory_size = 3
	exp := 0
	var inventory [inventory_size]string

	/* Add a junk item to inventory */
	inventory[0] = "Fishsticks"
	act := display_inventory(inventory[:])

	if exp != act {
		t.Fatalf("Expected %d got %d", exp, act)
	}
}

func Test_display_coffee_inventory(t *testing.T) {

	const inventory_size = 3
	exp := 1
	var inventory [inventory_size]string

	/* Add Coffee to the inventory */
	inventory[0] = "Coffee"
	act := display_inventory(inventory[:])

	if exp != act {
		t.Fatalf("Expected %d got %d", exp, act)
	}
}

func Test_display_cream_inventory(t *testing.T) {

	const inventory_size = 3
	exp := 2
	var inventory [inventory_size]string

	/* Add Cream to the inventory */
	inventory[0] = "Cream"
	act := display_inventory(inventory[:])

	if exp != act {
		t.Fatalf("Expected %d got %d", exp, act)
	}
}

func Test_display_sugar_inventory(t *testing.T) {

	const inventory_size = 3
	exp := 4
	var inventory [inventory_size]string

	/* Add Sugar to the inventory */
	inventory[0] = "Sugar"
	act := display_inventory(inventory[:])

	if exp != act {
		t.Fatalf("Expected %d got %d", exp, act)
	}
}

func Test_display_sugar_cream_inventory(t *testing.T) {

	const inventory_size = 3
	exp := 6
	var inventory [inventory_size]string

	/* Add sugar and cream to the inventory */
	inventory[0] = "Sugar"
	inventory[1] = "Cream"
	act := display_inventory(inventory[:])

	if exp != act {
		t.Fatalf("Expected %d got %d", exp, act)
	}
}

func Test_full_inventory(t *testing.T) {

	const inventory_size = 3
	exp := 7
	var inventory [inventory_size]string

	/* Add all proper items to inventory */
	inventory[0] = "Coffee"
	inventory[1] = "Cream"
	inventory[2] = "Sugar"

	act := display_inventory(inventory[:])

	if exp != act {
		t.Fatalf("Expected %d got %d", exp, act)
	}
}

func Test_inv_pos_invariance(t *testing.T) {

	const inventory_size = 3
	exp := 0
	var inventory_1 [inventory_size]string
	var inventory_2 [inventory_size]string

	inventory_1[0] = "Coffee"
	inventory_1[1] = "Sugar"

	inventory_2[0] = "Sugar"
	inventory_2[1] = "Coffee"

	act_1 := display_inventory(inventory_1[:])
	act_2 := display_inventory(inventory_2[:])

	if exp != (act_1 - act_2) {
		t.Fatalf("Expected %d got %d (%d - %d)", exp, act_1-act_2, act_1, act_2)
	}
}



/* NOTE: THIS WAS A DUMMY TEST */
/*func Test_adder(t *testing.T) {
	exp := 3 + 4
	act := adder(3, 4)

	if exp != act {
		t.Fatalf("Expected %d got %d", exp, act)
	}
}*/
