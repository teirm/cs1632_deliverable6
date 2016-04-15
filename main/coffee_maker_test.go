/* Name of Directory containing file */
package main 

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

func Test_adder(t *testing.T) {
	exp := 3 + 4
	act := adder(3,4)

	if exp != act {
		t.Fatal("Expected %d gog %d", exp, act)
	}
}
