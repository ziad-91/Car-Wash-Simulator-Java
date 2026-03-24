import java.util.LinkedList; // for the car queue (waiting area)
import java.util.Queue;
import java.util.Scanner; // to read user input
import javax.swing.*;  // for GUI components (windows, buttons, labels)
import java.awt.*;     // for layout (colors, borders)

// CarWashGUI class: sets up the colored GUI window and shows simulation status, queue, pumps, and log
// acts as the visual dashboard for user feedback and process visualization
class CarWashGUI extends JFrame {

    private JTextArea activityLog;   // a scrollable text box, shows the log of activities (car arrivals, service, etc.)
    private JLabel[] pumpLabels;     // array to show pump status ("Free", "Occupied", "Servicing")
    private JLabel[] queueLabels;    // array to show waiting area slots (which cars where)
    private int queueSize, numPumps; // user-specified waiting area size and number of pumps

    // constructor: sets up the window, panels, colors, and fonts
    public CarWashGUI(int numPumps, int queueSize) {
        this.numPumps = numPumps;
        this.queueSize = queueSize;

        setTitle("Car Wash Simulation"); // sets window title
        setSize(1000, 500);              // sets window size
        setDefaultCloseOperation(EXIT_ON_CLOSE); // when you click the X, the program exits
        setLayout(new BorderLayout(10,10)); // give padding between panels

        // log area: shows simulation progress in text form, scrollable
        activityLog = new JTextArea(8, 60);
        activityLog.setEditable(false); // makes it read-only
        activityLog.setFont(new Font("Consolas", Font.PLAIN, 15)); // sets font to "Consolas" (monospace), plain style, size 15
        activityLog.setBackground(new Color(240, 240, 240));
        add(new JScrollPane(activityLog), BorderLayout.SOUTH); // wraps the text area in a scroll pane (for scrolling) and adds it to the bottom of the window

        // pumps area: shows status of each service bay, colored and bordered for clarity
        JPanel pumpPanel = new JPanel(new GridLayout(1, numPumps, 10, 0));
        pumpPanel.setBorder(BorderFactory.createTitledBorder("Service Bays")); // gives labeled border
        pumpPanel.setBackground(new Color(200, 220, 255)); // light blue background for panel
        pumpLabels = new JLabel[numPumps];

	// loop to set up each pump label
        for (int i = 0; i < numPumps; i++) {
            pumpLabels[i] = new JLabel("Pump " + (i+1) + ": Free", JLabel.CENTER); // starts status "Free"
            pumpLabels[i].setOpaque(true);                 // so background color is visible
            pumpLabels[i].setFont(new Font("Arial", Font.BOLD, 18)); // big bold font
            pumpLabels[i].setBackground(new Color(170,255,170)); // green for available pump
            pumpLabels[i].setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY, 2)); // border for clarity
            pumpPanel.add(pumpLabels[i]);
        }
        add(pumpPanel, BorderLayout.NORTH);

        // queue area: visual waiting area slots, colored and separated for visibility
        JPanel queuePanel = new JPanel(new GridLayout(1, queueSize, 8, 0));
        queuePanel.setBorder(BorderFactory.createTitledBorder("Waiting Area")); // labeled border for area
        queuePanel.setBackground(new Color(230,245,255));  // very light blue panel background
        queueLabels = new JLabel[queueSize];
        for (int i = 0; i < queueSize; i++) {
            queueLabels[i] = new JLabel("Empty", JLabel.CENTER); // slot starts as "Empty"
            queueLabels[i].setOpaque(true);                      // make color visible
            queueLabels[i].setFont(new Font("Arial", Font.BOLD, 18)); // bold font for car names
            queueLabels[i].setBackground(new Color(230,230,230));     // grey for empty slot
            queueLabels[i].setBorder(BorderFactory.createLineBorder(Color.GRAY, 2)); // border for slot separation
            queuePanel.add(queueLabels[i]);
        }
        add(queuePanel, BorderLayout.CENTER);

        setVisible(true); // show the window when constructed
    }

    // log(): prints a message to the activity log area in the GUI (thread-safe via SwingUtilities)
    public void log(String msg) {
        SwingUtilities.invokeLater(() -> activityLog.append(msg + "\n"));
    }

    // updatePump(): visually updates the status/label and color of a pump (called when a car comes/goes)
    public void updatePump(int pumpIndex, String text) {
        SwingUtilities.invokeLater(() -> {
            pumpLabels[pumpIndex].setText(text);
            if (text.contains("Free")) {
                pumpLabels[pumpIndex].setBackground(new Color(170,255,170)); // green = free
            } else if (text.contains("Servicing")) {
                pumpLabels[pumpIndex].setBackground(new Color(255,255,140)); // yellow = servicing
            } else {
                pumpLabels[pumpIndex].setBackground(new Color(255,200,140)); // orange = occupied
            }
        });
    }

    // updateQueue(): refreshes all queue slot labels and colors when a car enters or leaves the waiting area
    public void updateQueue(String[] queueSnapshot) {
        SwingUtilities.invokeLater(() -> {
            for (int i = 0; i < queueSize; i++) {
                queueLabels[i].setText(queueSnapshot[i]);
                if(queueSnapshot[i].equals("Empty")) {
                    queueLabels[i].setBackground(new Color(230,230,230)); // grey = empty slot
                } else {
                    queueLabels[i].setBackground(new Color(180,210,255)); // light blue = occupied by car
                }
            }
        });
    }
}

// Semaphore class: acts like a counter for controlling access to resources
class Semaphore {
    private int value;  // current count of available resources

    // constructor: set the initial value (like number of empty pumps)
    public Semaphore(int initial) {
        value = initial;
    }

    // P() method (wait)
    public synchronized void P() {
        value--;  // decrement the counter (take one resource)
        if (value < 0) {
            try {
                wait();  // block the thread (calls Object.wait())
            } catch (InterruptedException e) { }
        }
    }

    // V() method (signal)
    public synchronized void V() {
        value++;  // increment the counter (add back one resource)
        if (value <= 0) {
            notify();  // wake up one waiting thread
        }
    }
}

// main class: sets up the simulation, initializes resources, and starts threads
// acts as the central hub for shared data and thread management
public class ServiceStation {
    // shared resources (accessed by all threads)
    private static Queue<String> waitingQueue;  // bounded buffer: holds car IDs ("C1")
    private static String[] bufferSlots;        // physical waiting slots for GUI visualization
    private static Semaphore empty;  // semaphore: counts empty slots in the queue
    private static Semaphore full;   // semaphore: counts cars waiting in the queue
    private static Semaphore pumps;  // semaphore: counts available service bays (pumps)
    private static Object mutex = new Object();  // mutex: lock for safe queue access (prevents race conditions)

    // simulation state variables
    private static int processedCars = 0;  // counter: tracks how many cars have been serviced
    private static boolean simulationEnded = false;  // flag: stops pumps when all cars are done

    // input variables (set from user input)
    private static int waitingCapacity;  // queue size (1-10, from input)
    private static int numOfPumps;  // number of pumps (at least 1, from input)
    private static String[] cars;  // array of car IDs from input
    private static int numOfCars;  // number of cars (from cars array length)

    public static CarWashGUI gui;  // reference to GUI dashboard

    // main method: gets input, initializes, and starts threads
    public static void main(String[] args) {
        Scanner input = new Scanner(System.in);  // for reading user input

        // prompt for waiting area capacity (must be 1-10)
        System.out.print("Enter garage waiting area size (1-10): ");
        waitingCapacity = input.nextInt();
        while (waitingCapacity < 1 || waitingCapacity > 10) {
            System.out.println("Invalid.");
            System.out.print("Enter garage waiting area size (1-10): ");
            waitingCapacity = input.nextInt();
        }

        // prompt for number of pumps (must be at least 1)
        System.out.print("Enter number of pumps/service bays (at least 1): ");
        numOfPumps = input.nextInt();
        while (numOfPumps < 1) {
            System.out.println("Invalid.");
            System.out.print("Enter number of pumps (at least 1): ");
            numOfPumps = input.nextInt();
        }

        // prompt for cars arriving in order (comma-separated)
        System.out.print("Enter cars arriving (order, comma-separated): ");
        String carsInput = input.next();
        cars = carsInput.split(",");  // split into array, e.g., "C1,C2,C3" -> ["C1", "C2", "C3"]
        numOfCars = cars.length;  // set number of cars from array length

        // initialize shared resources with input values
        waitingQueue = new LinkedList<>();  // empty queue
        bufferSlots = new String[waitingCapacity];          // array for GUI queue slots
        for (int i = 0; i < bufferSlots.length; i++) bufferSlots[i] = "Empty"; // all slots start empty

        gui = new CarWashGUI(numOfPumps, waitingCapacity);  // create and show the simulation GUI
        gui.updateQueue(bufferSlots);  // update the GUI queue panel

        empty = new Semaphore(waitingCapacity);  // starts with all slots empty
        full = new Semaphore(0);  // starts with no cars waiting
        pumps = new Semaphore(numOfPumps);  // starts with all pumps free

        // start pump threads (consumers) one per pump
        for (int i = 0; i < numOfPumps; i++) {
            new Pump(i + 1).start();  // Pump IDs: 1, 2, 3,...
        }

        // start car threads (producers) one per car, in order from input
        for (String carId : cars) {
            new Car(carId).start();  // Use car IDs from input array
            try {
                Thread.sleep(500);  // small delay to ensure cars arrive sequentially
            } catch (InterruptedException e) {}
        }

        input.close();  // close the scanner to free resources
    }

    // helper method: safely add a car to the queue (uses mutex)
    // also updates the GUI waiting slots for visualization
    public static void addToQueue(String car) {
        synchronized (mutex) {
            // put car in first available slot for GUI display (left to right)
            for (int i = 0; i < bufferSlots.length; i++) {
                if (bufferSlots[i].equals("Empty")) {
                    bufferSlots[i] = car;
                    break;
                }
            }
            waitingQueue.add(car);          // add to end of queue
            gui.updateQueue(bufferSlots);   // refresh GUI with new queue status
            // Removed: gui.log(car + " entered the queue"); // Not in old output
        }
    }

    // helper method: safely remove a car from the queue (uses mutex)
    // and update GUI (clear the slot for the serviced car)
    public static String removeFromQueue() {
        synchronized (mutex) {
            String car = waitingQueue.poll(); // remove from front of queue
            for (int i = 0; i < bufferSlots.length; i++) {
                if (bufferSlots[i].equals(car)) {
                    bufferSlots[i] = "Empty"; // mark slot as empty in GUI
                    break;
                }
            }
            gui.updateQueue(bufferSlots); // refresh GUI waiting area
            return car;
        }
    }

    // getters: allow other classes to access semaphores
    public static Semaphore getEmpty() {
        return empty;
    }
    public static Semaphore getFull() {
        return full;
    }
    public static Semaphore getPumps() {
        return pumps;
    }

    // increment processed cars counter and check if simulation is done
    public static void incrementProcessedCars() {
        synchronized (mutex) {  // lock to avoid race conditions on counter
            processedCars++;  // increase count
            if (processedCars == numOfCars) {
                simulationEnded = true;
                gui.log("All cars processed; simulation ends");  // final message
            }
        }
    }

    // getter to check if simulation has ended
    public static boolean isSimulationEnded() {
        return simulationEnded;
    }

    // get number of pumps (used in Car for "waiting" logic)
    public static int getNumOfPumps() {
        return numOfPumps;
    }
}

// Car class: Producer thread which simulates a car arriving, waiting for queue space, and joining the queue
// extends Thread to run concurrently
class Car extends Thread {
    private String id;  // for Car ID

    // constructor: set the car ID
    public Car(String id) {
        this.id = id;
    }

    @Override
    public void run() {
        // car arrives, wait for an empty slot in the queue
        ServiceStation.getEmpty().P();  // P on empty: block if queue is full

        // enter the queue safely and update GUI
        ServiceStation.addToQueue(id);  // add car to queue

        // log arrival
        ServiceStation.gui.log(id + " arrived"); // visual log rather than System.out

        // check if this car should print "arrived and waiting"
        int carNumber = Integer.parseInt(id.substring(1));  // get number from car ID ("C4" -> 4)
        if (carNumber > ServiceStation.getNumOfPumps()) {
            ServiceStation.gui.log(id + " arrived and waiting");
        }

        // signal that a car is now in the queue
        ServiceStation.getFull().V();  // V on full: notify pumps
    }
}

// Pump class: Consumer thread which simulates a pump taking a car from the queue, acquiring a bay, servicing, and releasing
// extends Thread to run concurrently
class Pump extends Thread {
    private int id;  // for Pump ID

    // constructor: set the pump ID
    public Pump(int id) {
        this.id = id;
    }

    @Override
    public void run() {
        // loop until simulation ends (stops infinite waiting)
        while (!ServiceStation.isSimulationEnded()) {
            // wait for a car in the queue
            ServiceStation.getFull().P();  // P on full: waits for a car, block if no cars

            // take the next car from the queue, update GUI status
            String car = ServiceStation.removeFromQueue();  // remove car safely
            ServiceStation.gui.log("Pump " + id + ": " + car + " Occupied");
            ServiceStation.gui.updatePump(id - 1, "Pump " + id + ": " + car + " Occupied");

            // log login, begin service, and update pump GUI status
            ServiceStation.gui.log("Pump " + id + ": " + car + " login");
            ServiceStation.gui.log("Pump " + id + ": " + car + " begins service at Bay " + id);
            ServiceStation.gui.updatePump(id - 1, "Pump " + id + ": " + car + " Servicing");

            // simulate service time (random 3-5 seconds)
            try {
                Thread.sleep(3000 + (int) (Math.random() * 2000));
            } catch (InterruptedException e) {}

            // log finish of service and update pump GUI status
            ServiceStation.gui.log("Pump " + id + ": " + car + " finishes service");
            ServiceStation.gui.log("Pump " + id + ": Bay " + id + " is now free");
            ServiceStation.gui.updatePump(id - 1, "Pump " + id + ": Free");

            // release the service bay
            ServiceStation.getPumps().V();  // V on pumps: free the bay

            // signal that a queue slot is now empty
            ServiceStation.getEmpty().V();  // V on empty: allow more cars

            // increment the processed cars counter and check if done
            ServiceStation.incrementProcessedCars();
        }
    }
}


/*  Sample Output (tested before adding GUI):
Enter garage waiting area size (1-10): 10
Enter number of pumps/service bays (at least 1): 3
Enter cars arriving (order, comma-separated): C1,C2,C3,C4,C5,C6,C7,C8,C9,C10
C1 arrived
Pump 1: C1 Occupied
Pump 1: C1 login
Pump 1: C1 begins service at Bay 1
C2 arrived
Pump 2: C2 Occupied
Pump 2: C2 login
Pump 2: C2 begins service at Bay 2
C3 arrived
Pump 3: C3 Occupied
Pump 3: C3 login
Pump 3: C3 begins service at Bay 3
C4 arrived
C4 arrived and waiting
C5 arrived
C5 arrived and waiting
C6 arrived
C6 arrived and waiting
C7 arrived
C7 arrived and waiting
C8 arrived
C8 arrived and waiting
C9 arrived
C9 arrived and waiting
Pump 3: C3 finishes service
Pump 3: Bay 3 is now free
Pump 3: C4 Occupied
Pump 3: C4 login
Pump 3: C4 begins service at Bay 3
C10 arrived
C10 arrived and waiting
Pump 1: C1 finishes service
Pump 1: Bay 1 is now free
Pump 1: C5 Occupied
Pump 1: C5 login
Pump 1: C5 begins service at Bay 1
Pump 2: C2 finishes service
Pump 2: Bay 2 is now free
Pump 2: C6 Occupied
Pump 2: C6 login
Pump 2: C6 begins service at Bay 2
Pump 1: C5 finishes service
Pump 1: Bay 1 is now free
Pump 1: C7 Occupied
Pump 1: C7 login
Pump 1: C7 begins service at Bay 1
Pump 2: C6 finishes service
Pump 2: Bay 2 is now free
Pump 2: C8 Occupied
Pump 2: C8 login
Pump 2: C8 begins service at Bay 2
Pump 3: C4 finishes service
Pump 3: Bay 3 is now free
Pump 3: C9 Occupied
Pump 3: C9 login
Pump 3: C9 begins service at Bay 3
Pump 2: C8 finishes service
Pump 2: Bay 2 is now free
Pump 2: C10 Occupied
Pump 2: C10 login
Pump 2: C10 begins service at Bay 2
Pump 3: C9 finishes service
Pump 3: Bay 3 is now free
Pump 1: C7 finishes service
Pump 1: Bay 1 is now free
Pump 2: C10 finishes service
Pump 2: Bay 2 is now free
All cars processed; simulation ends

*/
