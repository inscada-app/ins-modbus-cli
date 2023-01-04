package com.inscada.modbus.client.services;

import com.inscada.modbus.client.model.*;
import javafx.beans.property.ObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class DataBlockService {

    private final ObservableList<DataBlock> dataBlocks;
    private final FilteredList<DataBlock> filteredDataBlocks;
    private final SortedList<DataBlock> sortedDataBlocks;

    private final ObservableList<Tag> tags;

    private final FilteredList<Tag> filteredTags;

    private final SortedList<Tag> sortedTags;

    private String currentSearchTagFilter;
    private String currentDataBlockTagFilter;

    public DataBlockService() {
        this.dataBlocks = FXCollections.observableArrayList();
        this.filteredDataBlocks = new FilteredList<>(dataBlocks);
        this.sortedDataBlocks = new SortedList<>(filteredDataBlocks);
        this.tags = FXCollections.observableArrayList();
        this.filteredTags = new FilteredList<>(tags);
        this.sortedTags = new SortedList<>(filteredTags);
    }

    public List<DataBlock> getDataBlocks() {
        return Collections.unmodifiableList(dataBlocks);
    }

    public Optional<DataBlock> getDataBlock(String dataBlockName) {
        return dataBlocks.stream().filter(dataBlock -> dataBlock.getName().equalsIgnoreCase(dataBlockName)).findAny();
    }

    public ObservableList<DataBlock> findDataBlocks() {
        return sortedDataBlocks;
    }

    public Result<DataBlock> addDataBlock(DataBlock dataBlock) {
        List<String> validationErrors = new ArrayList<>();
        if (validateDataBlock(dataBlock, validationErrors, true)) {
            dataBlocks.add(dataBlock);
            return Result.createSuccessResult(dataBlock);
        } else {
            return Result.createErrorResult(validationErrors);
        }
    }

    public Result<DataBlock> updateDataBlock(String name, DataBlock dataBlock) {
        List<String> validationErrors = new ArrayList<>();
        DataBlock found = dataBlocks.stream()
                .filter(db -> db.getName().equalsIgnoreCase(name)).findFirst().orElse(null);
        if (found != null) {
            boolean isValid;
            boolean sameName = found.getName().equalsIgnoreCase(dataBlock.getName());
            if (sameName) {
                isValid = validateDataBlock(dataBlock, validationErrors, false);
            } else {
                isValid = validateDataBlock(dataBlock, validationErrors, true);
            }
            if (isValid) {
                dataBlocks.remove(found);
                dataBlocks.add(dataBlock);
                if (!sameName) {
                    getTags(found.getName()).forEach(tag ->
                            updateTag(found.getName(), tag.getName(),
                                    new Tag(dataBlock.getName(), tag.getName(), tag.getAddress(),
                                            tag.getType(), tag.isByteSwap(), tag.isWordSwap())));
                }
                return Result.createSuccessResult(dataBlock);
            } else {
                return Result.createErrorResult(validationErrors);
            }
        } else {
            return Result.createErrorResult(List.of("Data Block: Not found"));
        }
    }

    public Result<DataBlock> deleteDataBlock(String name) {
        DataBlock found = dataBlocks.stream()
                .filter(db -> db.getName().equalsIgnoreCase(name)).findFirst().orElse(null);
        if (found != null) {
            dataBlocks.remove(found);
            getTags(name).forEach(tag -> deleteTag(name, tag.getName()));
            return Result.createSuccessResult(found);
        } else {
            return Result.createErrorResult(List.of("Data Block: Not found"));
        }
    }

    public void changeDataBlockFilter(String filterText) {
        filteredDataBlocks.setPredicate(dataBlock -> {
            if (filterText == null || filterText.trim().isEmpty()) {
                return true;
            }
            String searchKeyword = filterText.toLowerCase();
            if (dataBlock.getName().toLowerCase().contains(searchKeyword)) {
                return true;
            } else if (Integer.toString(dataBlock.getAmount()).contains(searchKeyword)) {
                return true;
            } else if (Integer.toString(dataBlock.getStartAddress()).contains(searchKeyword)) {
                return true;
            } else if (dataBlock.getType().toString().toLowerCase().contains(searchKeyword)) {
                return true;
            } else {
                return false;
            }
        });
    }

    private boolean validateDataBlock(DataBlock dataBlock, List<String> errors, boolean sameNameCheck) {
        if (dataBlock.getName() == null || dataBlock.getName().isEmpty()) {
            errors.add("Data Block: Name cannot be empty");
        } else {
            if (sameNameCheck) {
                dataBlocks.stream()
                        .filter(db -> db.getName().equalsIgnoreCase(dataBlock.getName()))
                        .findAny()
                        .ifPresent(newDataBlock -> errors.add("Data Block: Name already exists"));
            }
        }
        if (dataBlock.getType() == null) {
            errors.add("Data Block: Type cannot be null");
        }
        if (dataBlock.getAmount() == null) {
            errors.add("Data Block: Amount cannot be empty");
        } else {
            if (dataBlock.getAmount() <= 0) {
                errors.add("Data Block: Amount should be greater than 0");
            }
        }
        if (dataBlock.getStartAddress() == null) {
            errors.add("Data Block: Start Address cannot be empty");
        } else {
            int startAddress = dataBlock.getStartAddress();
            int amount = dataBlock.getAmount();
            if (dataBlock.getType() == DataBlockType.Coils) {
                if (startAddress < 1 || startAddress > 9999) {
                    errors.add("Data Block: Coils start address must be in range [1-9999]");
                }
                if (startAddress + amount > 9999) {
                    errors.add(String.format("Amount must be in range [1-%d]", 9999 - startAddress));
                }
            } else if (dataBlock.getType() == DataBlockType.DiscreteInputs) {
                if (startAddress < 10001 || startAddress > 19999) {
                    errors.add("Data Block: Discrete Inputs start address must be in range [10001-19999]");
                }
                if (startAddress + amount > 19999) {
                    errors.add(String.format("Amount must be in range [1-%d]", 19999 - startAddress));
                }
            } else if (dataBlock.getType() == DataBlockType.HoldingRegisters) {
                if (startAddress < 40001 || startAddress > 49999) {
                    errors.add("Data Block: Holding Registers start address must be in range [40001-49999]");
                }
                if (startAddress + amount > 49999){
                    errors.add(String.format("Amount must be in range [1-%d]",49999 - startAddress));
                }
            } else if (dataBlock.getType() == DataBlockType.InputRegisters) {
                if (startAddress < 30001 || startAddress > 39999) {
                    errors.add("Data Block: Input Registers start address must be in range [30001-39999]");
                }
                if (startAddress + amount > 39999){
                    errors.add(String.format("Amount must be in range [1-%d]",39999 - startAddress));
                }
            }
        }
        return errors.isEmpty();
    }

    public List<Tag> getTags() {
        return Collections.unmodifiableList(tags);
    }

    public List<Tag> getTags(String dataBlockName) {
        return tags.stream().filter(t -> t.getDataBlockName().equalsIgnoreCase(dataBlockName))
                .collect(Collectors.toUnmodifiableList());
    }

    public ObservableList<Tag> findTags() {
        currentDataBlockTagFilter = null;
        filteredTags.setPredicate(getDataBlockTagFilterPredicate().and(getSearchTagFilterPredicate()));
        return sortedTags;
    }

    public ObservableList<Tag> findTags(String dataBlockName) {
        currentDataBlockTagFilter = dataBlockName;
        filteredTags.setPredicate(getDataBlockTagFilterPredicate().and(getSearchTagFilterPredicate()));
        return sortedTags;
    }

    public Result<Tag> addTag(Tag tag) {
        List<String> validationErrors = new ArrayList<>();
        if (validateTag(tag, validationErrors, true)) {
            tags.add(tag);
            return Result.createSuccessResult(tag);
        } else {
            return Result.createErrorResult(validationErrors);
        }
    }

    public Result<Tag> updateTag(String dataBlockName, String name, Tag tag) {
        List<String> validationErrors = new ArrayList<>();
        Tag found = tags.stream()
                .filter(t -> t.getDataBlockName().equalsIgnoreCase(dataBlockName)
                        && t.getName().equalsIgnoreCase(name)).findFirst().orElse(null);
        if (found != null) {
            boolean isValid;
            boolean sameName = found.getName().equalsIgnoreCase(tag.getName()) && found.getDataBlockName().equals(tag.getDataBlockName());
            if (sameName) {
                isValid = validateTag(tag, validationErrors, false);
            } else {
                isValid = validateTag(tag, validationErrors, true);
            }
            if (isValid) {
                tags.remove(found);
                tags.add(tag);
                return Result.createSuccessResult(tag);
            } else {
                return Result.createErrorResult(validationErrors);
            }
        } else {
            return Result.createErrorResult(List.of("Tag: Not found"));
        }
    }

    public Result<Tag> deleteTag(String dataBlockName, String name) {
        Tag found = tags.stream()
                .filter(tag -> tag.getDataBlockName().equalsIgnoreCase(dataBlockName)
                        && tag.getName().equalsIgnoreCase(name)).findFirst().orElse(null);
        if (found != null) {
            tags.remove(found);
            return Result.createSuccessResult(found);
        } else {
            return Result.createErrorResult(List.of("Tag: Not found"));
        }
    }

    public void changeTagFilter(String newValue) {
        currentSearchTagFilter = newValue;
        filteredTags.setPredicate(getDataBlockTagFilterPredicate().and(getSearchTagFilterPredicate()));
    }

    private Predicate<Tag> getDataBlockTagFilterPredicate() {
        return currentDataBlockTagFilter == null || currentDataBlockTagFilter.trim().isEmpty() ?
                t -> true : t -> t.getDataBlockName().equalsIgnoreCase(currentDataBlockTagFilter);
    }

    private Predicate<Tag> getSearchTagFilterPredicate() {
        return currentSearchTagFilter == null || currentSearchTagFilter.trim().isEmpty() ? t -> true :
                t -> {
                    String searchKeyword = currentSearchTagFilter.toLowerCase();
                    if (t.getName().toLowerCase().contains(searchKeyword)) {
                        return true;
                    } else if (Integer.toString(t.getAddress()).toLowerCase().contains(searchKeyword)) {
                        return true;
                    } else if (t.getType().toString().toLowerCase().contains(searchKeyword)) {
                        return true;
                    } else {
                        return false;
                    }
                };
    }

    private boolean validateTag(Tag tag, List<String> errors, boolean sameNameCheck) {
        DataBlock dataBlock = dataBlocks.stream()
                .filter(db -> db.getName().equalsIgnoreCase(tag.getDataBlockName())).findFirst().orElse(null);
        if (dataBlock == null) {
            errors.add("Tag: Select a data block");
        }
        if (tag.getType() == null) {
            errors.add("Tag: Select a tag type");
        } else {
            if (dataBlock != null &&
                    (dataBlock.getType() == DataBlockType.Coils || dataBlock.getType() == DataBlockType.DiscreteInputs) &&
                    tag.getType() != TagType.Boolean) {
                errors.add("Tag : Type can only selected as Boolean");
            }
            if (dataBlock != null &&
                    (tag.getType() == TagType.Boolean || tag.getType() == TagType.Byte || tag.getType() == TagType.Short) &&
                    tag.isWordSwap()) {
                errors.add("Tag: Type cannot use Word Swap");
            }
        }
        if (tag.getAddress() == null) {
            errors.add("Tag: Address cannot be empty");
        }
        if (dataBlock != null && tag.getAddress() != null) {
            int startAddress = dataBlock.getStartAddress();
            int amount = dataBlock.getAmount();
            int maxVal = startAddress + amount - 1;
            int tagAddress = tag.getAddress();
            if (tagAddress < startAddress || tagAddress > maxVal) {
                errors.add("Tag address must be [" + startAddress + "-" + maxVal + "]");
            }
        }
        if (tag.getName() == null) {
            errors.add("Tag: Name cannot be empty");
        } else {
            if (dataBlock != null && sameNameCheck) {
                tags.stream().filter(t -> t.getDataBlockName().equalsIgnoreCase(tag.getDataBlockName()) &&
                                t.getName().equalsIgnoreCase(tag.getName())).findFirst()
                        .ifPresent(t -> errors.add("Tag: Name already exists"));
            }
        }

        return errors.isEmpty();
    }

    public Result<Void> loadDataBlocks(List<DataBlock> loadDataBlocks) {
        dataBlocks.clear();
        List<String> errors = new ArrayList<>();
        if (loadDataBlocks != null) {
            for (DataBlock dataBlock : loadDataBlocks) {
                Result<DataBlock> dataBlockResult = addDataBlock(dataBlock);
                if (dataBlockResult.hasError()) {
                    errors.addAll(dataBlockResult.getErrors());
                }
            }
        }
        return errors.isEmpty() ? Result.createSuccessResult() : Result.createErrorResult(errors);
    }

    public Result<Void> loadTags(List<Tag> loadTags) {
        tags.clear();
        List<String> errors = new ArrayList<>();
        if (loadTags != null) {
            for (Tag tag : loadTags) {
                Result<Tag> tagResult = addTag(tag);
                if (tagResult.hasError()) {
                    errors.addAll(tagResult.getErrors());
                }
            }
        }
        return errors.isEmpty() ? Result.createSuccessResult() : Result.createErrorResult(errors);
    }

    public ObjectProperty<Comparator<? super DataBlock>> comparatorDataBlockProperty() {
        return sortedDataBlocks.comparatorProperty();
    }

    public ObjectProperty<Comparator<? super Tag>> comparatorTagProperty() {
        return sortedTags.comparatorProperty();
    }
}
