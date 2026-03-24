# Car Wash Simulation (Java)

A Java-based car wash simulation that models car arrivals, waiting queues, and service bays using multithreading, semaphores, and a Swing GUI.

## Features

- Multithreaded car and pump simulation
- Custom semaphore implementation
- Producer-consumer synchronization
- Queue visualization with Swing GUI
- Activity log showing arrivals, servicing, and completion

## Concepts Used

- Java Threads
- Semaphores
- Mutual Exclusion (Mutex)
- Producer-Consumer Problem
- Swing GUI
- Queue Data Structure

## How It Works

- Cars arrive in order and try to enter the waiting area
- Pumps take cars from the queue and service them
- If all service bays are busy, cars wait in the queue
- The GUI displays:
  - service bay status
  - waiting queue
  - activity log

## Project Structure

- `ServiceStation.java` — main source file containing:
  - `CarWashGUI`
  - `Semaphore`
  - `ServiceStation`
  - `Car`
  - `Pump`

## Screenshot

![Car Wash GUI](screenshots/gui-main.png)

## How to Run

1. Open the project in any Java IDE or terminal
2. Compile the file:

```bash
javac ServiceStation.java
