/*
 * Copyright  2001,2003-2005 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.apache.jmeld.tools.ant.taskdefs.condition;

import org.apache.jmeld.tools.ant.BuildException;

/**
 * Interface for conditions to use inside the &lt;condition&gt; task.
 *
 */
public interface Condition
{
  /**
   * Is this condition true?
   * 
   * @return true if the condition is true
   * @exception BuildException
   *              if an error occurs
   */
  boolean eval()
      throws BuildException;
}
