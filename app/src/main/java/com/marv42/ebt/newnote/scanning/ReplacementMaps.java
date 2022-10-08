/*
 Copyright (c) 2010 - 2022 Marvin Horter.
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
                {"K", "X"},
                {"%", "X"},
                {"@", "0"},
                {",", "1"},
                {"i", "1"},
                {"t", "1"},
                {"#", "4"},
                {"*", "5"},
                {"$", "5"},
                {">", "5"},
                {"É", "6"},
                {"?", "7"},
                {")", "7"},
                {"f", "7"},
                {"a", "8"},
                {"&", "8"},
                {"+", "9"}
        }).collect(Collectors.toMap(data -> data[0], data -> data[1]));
    }

    @NotNull
    static Map<String, String> getLetterMap() {
        return Stream.of(new String[][]{
                {"8", "A"},
                {"3", "B"},
                {"4", "N"},
                {"0", "O"}, // or D :-/
                {"$", "S"},
                {"1", "U"},
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
                {"P", "0"},
                {"i", "1"},
                {"\"", "11"},
                {"I", "1"},
                {"Q", "1"},
                {"t", "1"},
                {"R", "2"},
                {"Z", "2"},
                // {"s", "3"},
                // {"s", "5"},
                {"K", "4"},
                {"S", "5"},
                {"$", "5"},
                {"T", "7"},
                {"Y", "7"},
                {"a", "8"},
                {"A", "8"},
                {"B", "8"}
        }).collect(Collectors.toMap(data -> data[0], data -> data[1]));
    }
}
