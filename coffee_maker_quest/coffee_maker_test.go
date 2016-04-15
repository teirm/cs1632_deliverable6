/* Name of Directory containing file */
package coffee_maker_quest 

/* Testing imports */
import (
	. "github.com/onsi/ginkgo"
	. "github.com/onsi/gomega"
	"testing"
	"github.com/teirm/cs1632_deliverable6/coffee_maker_quest/"
)

/* Test method names have to start with 'Test' 
Different types of tests exist: *testing.<Type> 
See https://golang.org/pkg/testing/ for more info */

func Test_example(t *testing.T) {
	RegisterFailHandler(Fail)
	RunSpecs(t, "Coffee Maker Quest")
}

func Test_Adder(t *testing.T) {
	exp := 3 + 4
	act := Adder(3,4)

	if exp != act {
		t.Fatal("Expected %d gog %d", exp, act)
	}
}
