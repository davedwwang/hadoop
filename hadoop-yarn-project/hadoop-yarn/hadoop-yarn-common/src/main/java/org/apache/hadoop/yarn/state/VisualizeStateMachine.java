/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.hadoop.yarn.state;

import java.lang.reflect.Field;
import java.util.*;

import org.apache.commons.cli.*;
import org.apache.hadoop.classification.InterfaceAudience.Private;

@Private
public class VisualizeStateMachine {

  /**
   * @param classes list of classes which have static field
   *                stateMachineFactory of type StateMachineFactory
   * @return graph represent this StateMachine
   */
  public static Graph getGraphFromClasses(String graphName, List<String> classes, Set<String> startStates, Set<String> postStates)
      throws Exception {
    Graph ret = null;
    if (classes.size() != 1) {
      ret = new Graph(graphName);
    }
    for (String className : classes) {
      Class clz = Class.forName(className);
      Field factoryField = clz.getDeclaredField("stateMachineFactory");
      factoryField.setAccessible(true);
      StateMachineFactory factory = (StateMachineFactory) factoryField.get(null);
      if (classes.size() == 1) {
        return factory.generateStateGraph(graphName, startStates, postStates);
      }
      String gname = clz.getSimpleName();
      if (gname.endsWith("Impl")) {
        gname = gname.substring(0, gname.length()-4);
      }
      if (ret != null) {
        ret.addSubGraph(factory.generateStateGraph(gname, startStates, postStates));
      }
    }
    return ret;
  }

  public static void main(String [] args) throws Exception {
    run(args);
  }

  public Options buildGeneralOptions() throws Exception {
    OptionBuilder.withArgName("graph name").hasArg();
    Option gn = OptionBuilder.withArgName("<GraphName>")
            .hasArg()
            .withDescription("title in gv")
            .create("graphName");
    Option cl = OptionBuilder.withArgName("<class[,class[,...]]>")
            .hasArgs()
            .isRequired()
            .withDescription("class list")
            .withValueSeparator(',')
            .create("class");
    Option output = OptionBuilder.withArgName("<OutputFile>")
            .hasArg()
            .isRequired()
            .withDescription("output file")
            .create("outputFile");
    Option pre = OptionBuilder.withArgName("<preState[,preState[,...]]>")
            .hasArgs()
            .withValueSeparator(',')
            .withDescription("preState")
            .create("preState");
    Option post = OptionBuilder.withArgName("<postState[,postState[,...]]>")
            .hasArgs()
            .withValueSeparator(',')
            .withDescription("postState")
            .create("postState");
    Options opts = new Options();
    opts.addOption(gn);
    opts.addOption(cl);
    opts.addOption(output);
    opts.addOption(pre);
    opts.addOption(post);
    return opts;
  }
  public static void run(String [] args) throws Exception {
    VisualizeStateMachine vsm = new VisualizeStateMachine();
    Options options = vsm.buildGeneralOptions();
    GnuParser parser = new GnuParser();
    CommandLine cl = null;
    try {
      cl = parser.parse(options, args);
    } catch (Exception ex) {
      StringBuilder sb = new StringBuilder();
      boolean init = true;
      for (Object opt: options.getOptions()) {
        if (init) {
          init = false;
        }
        if (!init) {
          sb.append(" ");
        }
        Option option = (Option)opt;
        if (option.isRequired()) {
          sb.append("<-").append(option.getOpt()).append(" ").append(option.getArgName()).append(">");
        } else {
          sb.append("[-").append(option.getOpt()).append(" ").append(option.getArgName()).append("]");
        }
      }
      System.err.printf("Usage: %s %s\n", vsm.getClass().getName(), sb.toString());
      System.exit(1);
    }
    String[] classes = cl.getOptionValues("class");
    String outputFile = cl.getOptionValue("outputFile");
    String[] preStates = cl.getOptionValues("preState");
    String[] postStates = cl.getOptionValues("postState");
    String graphName = cl.getOptionValue("graphName");
    if (graphName == null) {
      graphName = "";
    }

    ArrayList<String> validClasses = new ArrayList<String>();
    for (String c : classes) {
      String vc = c.trim();
      if (vc.length()>0) {
        validClasses.add(vc);
      }
    }

    Set<String> startStateSet = new HashSet<>();
    Set<String> postStateSet = new HashSet<>();
    if (preStates != null) {
      for (String state: preStates) {
        startStateSet.add(state);
      }
    }

    if (postStates != null) {
      for (String state: postStates) {
        postStateSet.add(state);
      }
    }

    Graph g = getGraphFromClasses(graphName, validClasses, startStateSet, postStateSet);
    g.save(outputFile);
  }
}
