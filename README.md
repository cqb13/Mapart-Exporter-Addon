# Mapart Exporter

Easily save mapart as pngs

[Find more cool addons here](https://www.meteoraddons.com/)

## Features and Usage

Maps are saved to the `saved_maps` directory in the `.minecraft` folder

### Map Exporter Module

Exports all the maps you have selected as either a single image or a set of individual images

1. Turn on the `Mapart Selector` module
2. Middle click the maps you want to export
    - _Note: If exporting connected maps only select maps part of the mapart_
3. Export
    - If you are exporting 1x1 mapart run the `.export-selected-maps` command
    - If you are exporting mapart with larger dimensions run the `.export-selected-map [map-name]` command

### Inventory Map Export

Exports all the maps in your inventory as a set of individual images

1. Run the `.export-inventory-maps` command

### Linked Inventory Map Export

Exports maps in your inventory as a single image

1. Arrange maps in your inventory so that the top left corner of the mapart is in the top left corner of your inventory
2. Run the `.export-linked-inventory-maps` command

### Hand Map Export

Exports the map held in your hand

1. Run the `.export-mand` command
