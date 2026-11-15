package net.lyingice.datarecipegenerator.common;

/**
 * A Minecraft-free snapshot of one GUI slot.
 */
public record SlotStack(String itemId, int count) {
    public static final SlotStack EMPTY = new SlotStack("", 0);

    public SlotStack {
        itemId = itemId == null ? "" : itemId;
        if (count < 0) {
            count = 0;
        }
    }

    public static SlotStack of(String itemId, int count) {
        return new SlotStack(itemId, count);
    }

    public boolean isEmpty() {
        return this.itemId.isBlank() || this.count <= 0;
    }
}
