// this is the main package for the program
package main

// import all of the dependent classes
import (
	"fmt"
	"Game"
	"House"
	"Player"
)

type CoffeeMaker struct {

}

func (cm CoffeeMaker) runGameLoop() int {
	p Player
	h House
	g Game
	toReturn int
}

func main() {
	fmt.Println("Coffee Maker Quest 1.0")
	returnValue int = 0
	cm := CoffeeMaker{}
}