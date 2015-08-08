/**
 *  Lock Me After I Close
 *
 *  Copyright 2015 Kurt Sanders
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
import java.text.SimpleDateFormat;
import java.util.Date;
 
definition(
    name: "Lock Me After I Close",
    namespace: "SanderSoft",
    author: "Kurt Sanders",
    description: "Verify status of a door lock and a door contact, and ensure that we are safe & secure",
    category: "Safety & Security",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

preferences {
	section("When the door closes..."){
		input "contact", "capability.contactSensor", title: "Where?"
	}
	section("Lock the lock...") {
		input "lock","capability.lock", title: "Lock?"
	}
	section("Notify me...") {
	        input("recipients", "contact", title: "Send notifications to") {
            input "pushNotification", "bool", title: "Push notification", required: false, defaultValue: "true"
            input "phone", "phone", title: "Phone number", required: false
        }
	}
    section("Automatically lock the door after close...") {
    	input "minutesLater", "number", title: "Delay (in seconds):", required: true
    }
    section("Unlock it if the lock is manually engaged while the door is open...") {
    	input "minutesLater2", "number", title: "Delay (in seconds):", required: true
    }
}
def initialize() {
    log.debug "Auto Lock Door initialized."
	subscribe(contact, "contact", doorHandler, [filterEvents: false])
    subscribe(lock, "lock", doorHandler, [filterEvents: false])
    subscribe(lock, "unlock", doorHandler, [filterEvents: false])  
}
def installed() {
    initialize()
}
def updated() {
    unsubscribe()
    unschedule()
    initialize()
}

def lockDoor()
{
    lock.lock()
}
def unlockDoor()
{
    lock.unlock()
}
def doorHandler(evt)
{
    def ls = lock.latestValue("lock")
    def cs = contact.latestValue("contact")
    
    log.debug "Event: Name ${evt.name} is ${evt.value} value and created at: ${evt.date}"
    log.debug "latestContactValue: $cs"
    log.debug "latestLockValue: $ls"
    log.debug "The source of this event is: ${evt.source}"
    
// 	Handle the Various Events and Door/Lock Status
    
// 	Door has Been OPENED (Normal Door Opening Mode)
    if (evt.value == "open") { // If a person opens an unlocked door...
//        log.debug "Cancel the current task. Door is unlocked and somebody just opened it!"
        contactOpenHandler()
        unschedule( lockDoor ) // ...we don't need to lock it later.
        //Check Lock status when door was just opened
        if (lock.latestValue("lock") == "unlocked") {
            def msg = "Ohh.. no!!.. Lock status = $ls | Contact status = $cs"
            log.debug "$msg"
            pushNotificationHandler(msg)
        }
    }
// 	Door is currently OPEN, Lock was changed from UNLOCKED -> LOCKED (MISTAKE)
    else if ((contact.latestValue("contact") == "open") && (evt.value == "locked")) {   
//      log.debug "Door is in open status and somebody just locked the lock.  Mistake, unlocking the lock after !"
        runIn( minutesLater2, unlockDoor )   // ...schedule (in minutes) to unlock...  We don't want the door to be closed while the lock is engaged. 
    }
// 	Door is currently OPEN, Lock was changed from LOCKED -> UNLOCKED (MANUALLY UNLOCKED THE LOCK)
    else if ((contact.latestValue("contact") == "open") && (evt.value == "unlocked")) { // If the door is open and a person unlocks it then...
//      log.debug "Cancel the current task. Door is open and somebody just manually unlocked the lock!"
        unschedule( unlockDoor ) // ...we don't need to unlock it later.
	}
    
// 	Door is currently CLOSED, Lock was changed from UNLOCKED to LOCKED (NORMAL CLOSING & LOCKING MODE)
	else if ((contact.latestValue("contact") == "closed") && (evt.value == "locked")) { // If the door is closed and a person manually locks it then...
//      log.debug "Cancel the current task. Door is closed and somebody just locked it!"
        unschedule( lockDoor ) // ...we don't need to lock it later.
    }
// 	Door is currently CLOSED, Lock was changed from LOCKED to UNLOCKED
    else if ((contact.latestValue("contact") == "closed") && (evt.value == "unlocked")) { // If the door is closed and a person unlocks it then...
//      log.debug "Door is closed and somebody just unlocked it.  Locking the door!"
        log.debug "Re-arming lock in (${minutesLater}s)."
        runIn( minutesLater, lockDoor ) // ...schedule (in minutes) to lock.
    }
// 	Door has been CLOSED and Lock is UNLOCKED (Normal Close Door Mode)
	else if ((lock.latestValue("lock") == "unlocked") && (evt.value == "closed")) { // If a person closes an unlocked door...
//      log.debug "Door is unlocked and somebody just closed it.  Locking the door!"
        contactCloseHandler()
        log.debug "Re-arming lock in (${minutesLater}s)."
        runIn( minutesLater, lockDoor ) // ...schedule (in minutes) to lock.
    }
    else {
    // Unexpected Door and/or Lock state(s)
		def msg = "Ohh.. no!!.. Lock status = $ls | Contact status = $cs"
        log.debug "$msg"
        pushNotificationHandler(msg)
        msg = "Event Debug: Name ${evt.name} is ${evt.value} value"
        pushNotificationHandler(msg)
        // Reset Any Orphaned Door Scheduled Events
		unschedule( lockDoor )
        unschedule( unlockDoor )
		updateLastRunDT()
    }
}

def contactCloseHandler() { //Specified contact has been event closed
    //Set Timezone to New York for me!
    TimeZone.setDefault(TimeZone.getTimeZone('America/New_York'))
    SimpleDateFormat format = new SimpleDateFormat(
                "EEE, d MMM, hh:mm a");
    //Generate the door history report
    // Format a message
    def today = new Date()
    def currentTime = now()
    log.debug "currentTime = $currentTime"
    log.debug "OpenStartDT in Close Event = ${state.OpenStartDT}"    
    log.debug "OpenStartDT in Close Event = ${format.format(new Date(state.OpenStartDT))}"
    log.debug "OpenStartDT in Close Event after format.format = ${state.OpenStartDT}"    
    def int DoorOpenTime = (currentTime - state.OpenStartDT)/1000
	log.debug "DoorOpenTime = ${DoorOpenTime}"
	log.debug "DoorOpenTime converted = ${timeConversion(DoorOpenTime)}"
	def msg = ""
    msg = "The Front Door has been closed and locked, it was opened at ${format.format(new Date(state.OpenStartDT))} for ${timeConversion(DoorOpenTime)}"
	pushNotificationHandler(msg)
}

def contactOpenHandler() { //Specified contact has been event opened
    //Set Timezone to New York for me!
    TimeZone.setDefault(TimeZone.getTimeZone('America/New_York'))
    def today = new Date()
    SimpleDateFormat format = new SimpleDateFormat(
                "EEE, d MMM, hh:mm a");
	updateLastRunDT()
	log.debug "OpenStartDT in Open Event = ${state.OpenStartDT}"    
    log.debug "OpenStartDT in Open Event = ${format.format(new Date(state.OpenStartDT))}"    
    log.debug "OpenStartDT in Open Event after format.format = ${state.OpenStartDT}"    
	def msg = "Your front door was opened on ${format.format(today)}"
	pushNotificationHandler(msg)
}

def updateLastRunDT() {
	log.debug "Updating OpenStartDT to ${now()}"
	state.OpenStartDT = now()
}

private static String timeConversion(int totalSeconds) {

    final int MINUTES_IN_AN_HOUR = 60;
    final int SECONDS_IN_A_MINUTE = 60;
	def HMS = "";

    int seconds = totalSeconds % SECONDS_IN_A_MINUTE;
    int totalMinutes = totalSeconds / SECONDS_IN_A_MINUTE;
    int minutes = totalMinutes % MINUTES_IN_AN_HOUR;
    int hours = totalMinutes / MINUTES_IN_AN_HOUR;
    if (hours > 0) {
	    return hours + " hrs " + minutes + " mins " + seconds + " secs";
    }
    else if (minutes>0) {
        return minutes + " mins " + seconds + " secs";
	}
    else {
	    return seconds + " secs";
    }
}

def pushNotificationHandler(msgString) {
    //Send out Notifications
	log.debug "$msgString"
    if (pushNotification) {
        sendPush("${msgString}")
    }
            if (phone1 != null && phone != "") {
        sendSms(phone,"${msgString}")
    }
}