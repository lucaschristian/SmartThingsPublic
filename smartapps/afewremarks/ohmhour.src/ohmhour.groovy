/**
 *  Ohm Hour
 *
 *  Copyright 2015 Mark West
 *  Version 1.0 8/8/15
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
definition(
    name: "OhmHour",
    namespace: "afewremarks",
    author: "Mark West",
    description: "Integration into ohm connect to turn off plugs/switches and dim lights during peak energy events",
    category: "",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("Select ") {
		input "masters", "capability.switch", 
			multiple: false, 
			title: "Ohm Connect Switch...", 
			required: true
	}
    section("During OhmHour turn off these switches...") {
		input "slaves", "capability.switch", 
			multiple: true, 
			title: "On/Off Switch(es)...", 
			required: false
	}
    section("During OhmHour dim these switches...") {
		input(
            	name		: "dimmers"
                ,multiple	: true
                ,required	: false
                ,type		: "capability.switchLevel"
            )
        input(
                name		: "dimlevel"
                ,title		: "To this level..."
                ,multiple	: false
                ,required	: false
                ,type		: "enum"
                ,options	: ["10":"10%","20":"20%","30":"30%","40":"40%","50":"50%"]
            )
	}
}

def installed() {
	log.debug "Installed with settings: ${settings}"

	initialize()
    subscribe(masters, "switch.on", switchOnHandler)
	subscribe(masters, "switch.off", switchOffHandler)
}

def updated() {
	log.debug "Updated with settings: ${settings}"

	unsubscribe()
	initialize()
    
	subscribe(masters, "switch.on", switchOnHandler)
	subscribe(masters, "switch.off", switchOffHandler)
	log.info "subscribed to all of switches events"
}

def initialize() {
	// TODO: subscribe to attributes, devices, locations, etc.
}


def switchOffHandler(evt) {
	log.info "switchoffHandler Event: ${evt.value}"
    
    	slaves.each {
        	//log.debug it.currentValue("switch")
            //log.debug state.switchprevious[it.id].switch
        
        if( state.switchprevious[it.id].switch == it.currentValue("switch")){
        	log.info "Some lights states have changed so not returning to previous state"
        }else{
        	it.on()
        }
	}
    
        dimmers.each {
            //log.debug it.currentValue("level")
            //log.debug state.switchprevious[it.id].level
        
        //Check if the level has changed since ohm hour has started       
        
        if( it.currentValue("level") == "on" && dimlevel != it.currentValue("level")){
        	log.info "Some dimmers levels have changed so not returning to previous level"
        }else{
        	it.setLevel(state.switchprevious[it.id].level)
        }
	}
}

def switchOnHandler(evt) {
	log.info "switchOnHandler Event: ${evt.value}"
    
    state.switchprevious = [:]
	slaves.each {
		state.switchprevious[it.id] = [
        "switch": it.currentValue("switch")
        ]
	}
    
    state.dimmerprevious = [:]
	dimmers.each {
		state.dimmerprevious[it.id] = [
        "switch": it.currentValue("switch"),
        "level": it.currentValue("level")        
		]
               
       if(it.currentValue("switch") == "on"){
        it.setLevel(dimlevel)
        }
	}
    
	slaves?.off()
}