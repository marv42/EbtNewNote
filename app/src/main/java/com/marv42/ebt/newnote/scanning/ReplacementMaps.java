/*
 Copyright (c) 2010 - 2020 Marvin Horter.
 All rights reserved. This program and the accompanying materials
 are made available under the terms of the GNU Public License v2.0
 which accompanies this distribution, and is available at
 http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package com.marv42.ebt.newnote.scanning;

import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class ReplacementMaps {

    @NotNull
    static Map<String, String> getAmbiguousMap() {
        return Stream.of(new String[][]{
                // {"$", "S"},
                // {"$", "5"},
                {"K", "X"},
                {"%", "X"},
                {"@", "0"},
                {"Ö", "0"},
                {"i", "1"},
                {"t", "1"},
                {"#", "4"},
                {"*", "5"},
                {"$", "5"},
                {">", "5"},
                {"?", "7"},
                {")", "7"},
                {"f", "7"},
                {"a", "8"},
                {"&", "8"}
        }).collect(Collectors.toMap(data -> data[0], data -> data[1]));
    }

    @NotNull
    static Map<String, String> getLetterMap() {
        return Stream.of(new String[][]{
                {"8", "A"},
                {"3", "B"},
                {"É", "E"},
                {"4", "N"},
                {"0", "D"}, // is O legit?
                {"1", "U"},
                {"K", "X"},
                {"2", "Z"}
                // {"1", "Z"}
        }).collect(Collectors.toMap(data -> data[0], data -> data[1]));
    }

    @NotNull
    static Map<String, String> getDigitMap() {
        return Stream.of(new String[][]{
                {"D", "0"},
                {"O", "0"},
                {"o", "0"},
                {"i", "1"},
                {"\"", "11"},
                {"I", "1"},
                {"t", "1"},
                {"R", "2"},
                {"Z", "2"},
                // {"s", "3"},
                // {"s", "5"},
                {"S", "5"},
                {"T", "7"},
                {"Y", "7"},
                {"a", "8"},
                {"A", "8"},
                {"B", "8"}
        }).collect(Collectors.toMap(data -> data[0], data -> data[1]));
    }
}
