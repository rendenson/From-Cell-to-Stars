package com.celltostars.data;

/** Everything needed to draw a cell: colours, pattern, mouth and parts. Used for the player, the mate and AI cells. */
public record CellLook(int primary, int secondary, Pattern pattern, Diet diet, int flagella, int spikes, boolean membrane) {
}
