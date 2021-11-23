package com.sixshop.payment.model;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.ArrayList;
import java.util.List;

@JsonSerialize
public class EmptyObject {
    public static EmptyObject OBJECT = new EmptyObject();
    public static List LIST = new ArrayList();
}
