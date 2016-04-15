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
	"testing"
)

/* Test method names have to start with 'Test'
Different types of tests exist: *testing.<Type>
See https://golang.org/pkg/testing/ for more info */

func Test_example(t *testing.T) {
	RegisterFailHandler(Fail)
	RunSpecs(t, "Coffee Maker Quest")
}

func Test_i_love_cyrus(t *testing.T) {
	var expected = 0
	var actual = 1

	if expected != actual {
		t.Fatalf("Expected %d but got %d", expected, actual)
	}
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
func Test_display_instructions(t *testing.T) {

	exp := true
	act := display_instructions()

	if exp != act {
		t.Fatalf("Expected %d got %d", exp, act)
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
