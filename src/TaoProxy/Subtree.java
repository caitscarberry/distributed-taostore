package TaoProxy;

import com.google.common.collect.Multiset;

import java.util.Set;

public interface Subtree {
    void initRoot();

    /**
     * @brief Method to add a path to the subtree
     * @param path
     */
    void addPath(Path path);

    /**
     * @brief Method to get requested path from subtree
     * note: shallow copy
     * @param pathID
     * @return path with ID == pathID
     */
    Path getPath(long pathID);

    /**
     * @brief Method to get a bucket in the subtree the contains the specified block with block ID == blockID
     * @param blockID
     * @return
     */
    Bucket getBucketWithBlock(long blockID);

    /**
     * @brief Method that clears a path and returns it in order to be flushed
     * @param pathID
     * @return
     */
    Path getPathToFlush(long pathID);

    void mapBlockToBucket(long blockID, Bucket bucket);

    void deleteNodes(long pathID, long minTime, Set<Long> pathReqMultiSet);
}
