/*
 *    Copyright 2018 University of Michigan
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package org.verdictdb.core.aggresult;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AggregateGroup implements Serializable {

  List<String> attributeNames;

  List<Object> attributeValues;

  public AggregateGroup(List<String> attributeNames, List<Object> attributeValues) {
    this.attributeNames = new ArrayList<>();
    for (String name : attributeNames) {
      this.attributeNames.add(name.toLowerCase());
    }
    this.attributeValues = attributeValues;
  }

  public List<String> getAttributeNames() {
    return attributeNames;
  }

  public List<Object> getAttributeValues() {
    return attributeValues;
  }

  public static AggregateGroup empty() {
    return new AggregateGroup(Arrays.<String>asList(), Arrays.asList());
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((attributeNames == null) ? 0 : attributeNames.hashCode());
    result = prime * result + ((attributeValues == null) ? 0 : attributeValues.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    AggregateGroup other = (AggregateGroup) obj;
    if (attributeNames == null) {
      if (other.attributeNames != null) return false;
    } else if (!attributeNames.equals(other.attributeNames)) return false;
    if (attributeValues == null) {
      if (other.attributeValues != null) return false;
    } else if (!attributeValues.equals(other.attributeValues)) return false;
    return true;
  }

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
  }
}
