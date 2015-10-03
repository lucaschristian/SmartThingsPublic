/**
 *  REST Endpoint
 *
 *  Author: markewest@gmail.com
 *  Date: 2013-12-08
 */

// Automatically generated. Make future change here.
definition(
    name: "REST Endpoint",
    namespace: "afewremarks",
    author: "markewest@gmail.com",
    description: "Example Rest Endpoint",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience%402x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience%402x.png",
    oauth: true)

preferences {
	section("Allow Endpoint to Control These Things...") {
		input "switches", "capability.switch", title: "Which Switches?", multiple: true
        input "locks", "capability.lock", title: "Which Locks?", multiple: true
        input "sensor","capability.temperatureMeasurement", title: "Which temperature Sensor?", multiple:true
	}
}

mappings {

	path("/switches") {
		action: [
			GET: "listSwitches"
		]
	}
	path("/switches/:id") {
		action: [
			GET: "showSwitch"
		]
	}
	path("/switches/:id/:command") {
		action: [
			GET: "updateSwitch"
		]
	}
    path("/sensor"){
    	action: [
        	GET: "listSensor"
            ]
        }
    path("/sensor/:id") {
		action: [
			GET: "showSensor"
		]
	}
    path("/sensor/:id/temp") {
		action: [
			GET: "showSensorTemp"
		]
	}
	path("/locks") {
		action: [
			GET: "listLocks"
		]
	}
	path("/locks/:id") {
		action: [
			GET: "showLock"
		]
	}
	path("/locks/:id/:command") {
		action: [
			GET: "updateLock"
		]
	}    
    
}

def installed() {
//	subscribe(sensor, "temperature", temperatureHandler)
}

def updated() {
//	subscribe(sensor, "temperature", temperatureHandler)
}

def temperatureHandler(temp) {
	log.trace "temperature: $temp.value, $temp"
    }

//switches
def listSwitches() {
	switches.collect{device(it,"switch")}
}

def showSwitch() {
	show(switches, "switch")
}
void updateSwitch() {
	update(switches)
}
//Sensors
def listSensor() {
	sensor.collect{device(it,"sensor")}
}

def showSensor() {
	show(sensor, "sensor")
}
/*void showSensorTemp() {
	showtemp(sensor, "sensor")
}*/
//locks
def listLocks() {
	locks.collect{device(it,"lock")}
}

def showLock() {
	show(locks, "lock")
}

void updateLock() {
	update(locks)
}



def deviceHandler(evt) {}

private void update(devices) {
	log.debug "update, request: params: ${params}, devices: $devices.id"
    
    
	//def command = request.JSON?.command
    def command = params.command
    //let's create a toggle option here
	if (command) 
    {
		def device = devices.find { it.id == params.id }
		if (!device) {
			httpError(404, "Device not found")
		} else {
        	if(command == "toggle")
       		{
            	if(device.currentValue('switch') == "on")
                  device.off();
                else
                  device.on();
       		}
       		else
       		{
				device."$command"()
            }
		}
	}
}

private show(devices, type) {
	def device = devices.find { it.id == params.id }
	if (!device) {
		httpError(404, "Device not found")
	}
	else {
		def attributeName = type == "motionSensor" ? "motion" : type
		def s = device.currentState(attributeName)
		[id: device.id, label: device.displayName, value: s?.value, unitTime: s?.date?.time, temp: device.currentValue('temperature'), humidity: device.currentValue('humidity'),type: type]
	}
}


private device(it, type) {
	it ? [id: it.id, label: it.label, type: type] : null
}
