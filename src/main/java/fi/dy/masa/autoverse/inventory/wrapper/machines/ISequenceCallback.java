package fi.dy.masa.autoverse.inventory.wrapper.machines;

public interface ISequenceCallback
{
    /**
     * Called from a SequenceMatcher, when the sequence is being configured.<br>
     * Called for each slot, using the <b>callbackId</b> that was set to the SequenceMatcher.<br>
     * <b>NOTE:</b> This callback is NOT called for the end marker item in SequenceMatcherVariable!
     * @param callbackId the id of the SequenceMatcher calling this callback
     * @param slot the slot number currently being configured with an item
     * @param finished true, if this item finishes the configuration of the calling SequenceMatcher
     */
    public void onConfigureSequenceSlot(int callbackId, int slot, boolean finished);
}
