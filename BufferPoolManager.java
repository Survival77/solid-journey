import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

/* ===================== Page Structure ===================== */
class Page {
    int pageId;
    String data;
    boolean dirty;
    int pinCount;

    Page() {
        this.pageId = -1;
        this.data = "";
        this.dirty = false;
        this.pinCount = 0;
    }
}

/* ===================== Disk Manager ===================== */
class DiskManager {
    // Simulate writing a page to disk
    void writePage(int pageId, String data) {
        try (FileWriter writer = new FileWriter("disk.txt", true)) {
            writer.write("Page " + pageId + ": " + data + System.lineSeparator());
        } catch (IOException e) {
            System.err.println("Failed to write page " + pageId + " to disk: " + e.getMessage());
        }
    }

    // Simulate reading a page from disk
    String readPage(int pageId) {
        return "Data_of_Page_" + pageId;
    }
}

/* ===================== LRU Replacer ===================== */
class LRUReplacer {
    private final LinkedList<Integer> lruList = new LinkedList<>();

    void access(int frameId) { // mark recently used
        lruList.remove(Integer.valueOf(frameId));
        lruList.addFirst(frameId);
    }

    int victim() { // least recently used frame or -1
        return lruList.isEmpty() ? -1 : lruList.removeLast();
    }

    void remove(int frameId) {
        lruList.remove(Integer.valueOf(frameId));
    }
}

/* ===================== Buffer Pool Manager ===================== */
public class BufferPoolManager {
    private final int poolSize;
    private final Page[] frames;
    private final Map<Integer, Integer> pageTable;
    private final LRUReplacer replacer;
    private final DiskManager disk;

    private int hits = 0;
    private int misses = 0;

    BufferPoolManager(int size) {
        this.poolSize = size;
        this.frames = new Page[size];
        for (int i = 0; i < size; i++) {
            frames[i] = new Page();
        }
        this.pageTable = new HashMap<>();
        this.replacer = new LRUReplacer();
        this.disk = new DiskManager();
    }

    // Fetch a page into memory
    Page fetchPage(int pageId) {
        // Cache HIT
        if (pageTable.containsKey(pageId)) {
            hits++;
            int frameId = pageTable.get(pageId);
            if (frames[frameId].pinCount == 0) {
                replacer.remove(frameId);
            }
            frames[frameId].pinCount++;
            return frames[frameId];
        }

        // Cache MISS
        misses++;

        int frameId = findFreeFrame();
        if (frameId == -1) {
            frameId = evictPage();
            if (frameId == -1) {
                System.out.println("No frame available for eviction");
                return null;
            }
        }

        // Load page from disk
        frames[frameId].pageId = pageId;
        frames[frameId].data = disk.readPage(pageId);
        frames[frameId].dirty = false;
        frames[frameId].pinCount = 1;

        pageTable.put(pageId, frameId);

        return frames[frameId];
    }

    // Unpin a page
    void unpinPage(int pageId, boolean isDirty) {
        if (!pageTable.containsKey(pageId)) {
            return;
        }

        int frameId = pageTable.get(pageId);
        if (frames[frameId].pinCount > 0) {
            frames[frameId].pinCount--;
        }

        if (isDirty) {
            frames[frameId].dirty = true;
        }

        // Only unpinned frames are candidates for eviction
        if (frames[frameId].pinCount == 0) {
            replacer.access(frameId);
        }
    }

    // Flush all dirty pages to disk
    void flushAll() {
        for (Page frame : frames) {
            if (frame.dirty) {
                disk.writePage(frame.pageId, frame.data);
                frame.dirty = false;
            }
        }
    }

    // Print cache statistics
    void printMetrics() {
        int total = hits + misses;
        double hitRate = total != 0 ? (double) hits / total : 0.0;

        System.out.println("\n--- Performance Metrics ---");
        System.out.println("Hits: " + hits);
        System.out.println("Misses: " + misses);
        System.out.println("Hit Rate: " + hitRate);
    }

    // Find an empty frame
    private int findFreeFrame() {
        for (int i = 0; i < poolSize; i++) {
            if (frames[i].pageId == -1) {
                return i;
            }
        }
        return -1;
    }

    // Evict a page using LRU
    private int evictPage() {
        int frameId = replacer.victim();
        if (frameId == -1) {
            return -1;
        }

        Page victim = frames[frameId];
        if (victim.pinCount > 0) {
            return -1;
        }

        if (victim.dirty) {
            disk.writePage(victim.pageId, victim.data);
        }

        pageTable.remove(victim.pageId);
        frames[frameId] = new Page();
        return frameId;
    }

    /* ===================== Main Function ===================== */
    public static void main(String[] args) {
        BufferPoolManager bpm = new BufferPoolManager(3); // Buffer size = 3 frames

        Page p1 = bpm.fetchPage(1);
        bpm.unpinPage(1, true);

        Page p2 = bpm.fetchPage(2);
        bpm.unpinPage(2, false);

        Page p3 = bpm.fetchPage(3);
        bpm.unpinPage(3, false);

        Page p4 = bpm.fetchPage(4); // Triggers LRU eviction
        bpm.unpinPage(4, true);

        bpm.flushAll();
        bpm.printMetrics();
    }
}
