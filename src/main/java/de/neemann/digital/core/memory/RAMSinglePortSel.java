/*
 * Copyright (c) 2017 Helmut Neemann
 * Use of this source code is governed by the GPL v3 license
 * that can be found in the LICENSE file.
 */
package de.neemann.digital.core.memory;

import de.neemann.digital.core.*;
import de.neemann.digital.core.element.Element;
import de.neemann.digital.core.element.ElementAttributes;
import de.neemann.digital.core.element.ElementTypeDescription;
import de.neemann.digital.core.element.Keys;
import de.neemann.digital.draw.elements.PinException;

import static de.neemann.digital.core.element.PinInfo.input;

/**
 * RAM module with a single port to read and write data and a select input.
 * This allows to build a bigger RAM with smaller RAMS and an additional address decoder.
 */
public class RAMSinglePortSel extends Node implements Element, RAMInterface {

    /**
     * The RAMs {@link ElementTypeDescription}
     */
    public static final ElementTypeDescription DESCRIPTION = new ElementTypeDescription(RAMSinglePortSel.class,
            input("A"),
            input("CS"),
            input("WE").setClock(),
            input("OE"))
            .addAttribute(Keys.ROTATE)
            .addAttribute(Keys.BITS)
            .addAttribute(Keys.ADDR_BITS)
            .addAttribute(Keys.LABEL)
            .addAttribute(Keys.IS_PROGRAM_MEMORY)
            .addAttribute(Keys.INVERTER_CONFIG);

    private final int bits;
    private final int addrBits;
    private final int size;
    private final String label;
    private final ObservableValue dataOut;
    private final boolean isProgramMemory;
    private DataField memory;
    private ObservableValue addrIn;
    private ObservableValue csIn;
    private ObservableValue weIn;
    private ObservableValue oeIn;
    private ObservableValue dataIn;

    private int readAddr;
    private int writeAddr;
    private boolean cs;
    private boolean oe;
    private boolean we;
    private boolean lastWrite;

    /**
     * Creates a new instance
     *
     * @param attr the elements attributes
     */
    public RAMSinglePortSel(ElementAttributes attr) {
        super(true);
        bits = attr.get(Keys.BITS);
        addrBits = attr.get(Keys.ADDR_BITS);
        size = 1 << addrBits;
        memory = createDataField(attr, size);
        label = attr.getLabel();
        dataOut = new ObservableValue("D", bits)
                .setToHighZ()
                .setPinDescription(DESCRIPTION)
                .setBidirectional();
        isProgramMemory = attr.get(Keys.IS_PROGRAM_MEMORY);
    }

    /**
     * creates the data field to use
     *
     * @param attr the elements attributes
     * @param size the size of the memory
     * @return the memory to use
     */
    protected DataField createDataField(ElementAttributes attr, int size) {
        return new DataField(size);
    }

    @Override
    public void setInputs(ObservableValues inputs) throws NodeException {
        addrIn = inputs.get(0).checkBits(addrBits, this).addObserverToValue(this);
        csIn = inputs.get(1).checkBits(1, this).addObserverToValue(this);
        weIn = inputs.get(2).checkBits(1, this).addObserverToValue(this);
        oeIn = inputs.get(3).checkBits(1, this).addObserverToValue(this);
        dataIn = inputs.get(4).checkBits(bits, this);
    }

    @Override
    public void readInputs() throws NodeException {
        cs = csIn.getBool();
        if (cs) {
            readAddr = (int) addrIn.getValue();
            oe = oeIn.getBool();
        }

        we = weIn.getBool();
        boolean write = cs && we;
        if (write && !lastWrite)
            writeAddr = (int) addrIn.getValue();

        if (!write && lastWrite) {
            long data = dataIn.getValue();
            memory.setData(writeAddr, data);
        }
        lastWrite = write;
    }

    @Override
    public void writeOutputs() throws NodeException {
        if (cs && oe && !we) {
            dataOut.setValue(memory.getDataWord(readAddr));
        } else {
            dataOut.setToHighZ();
        }
    }

    @Override
    public ObservableValues getOutputs() throws PinException {
        return dataOut.asList();
    }

    @Override
    public DataField getMemory() {
        return memory;
    }

    @Override
    public String getLabel() {
        return label;
    }

    @Override
    public int getSize() {
        return size;
    }

    @Override
    public int getDataBits() {
        return bits;
    }

    @Override
    public int getAddrBits() {
        return addrBits;
    }

    /**
     * Sets the rams data
     *
     * @param data the data to set
     */
    public void setData(DataField data) {
        this.memory = data;
    }

    @Override
    public boolean isProgramMemory() {
        return isProgramMemory;
    }

    @Override
    public void setProgramMemory(DataField dataField) {
        memory.setDataFrom(dataField);
    }
}
