
/*
  Copyright 2011-2016 IBM
	Licensed under the Apache License, Version 2.0 (the "License");
	you may not use this file except in compliance with the License.
	You may obtain a copy of the License at
	http://www.apache.org/licenses/LICENSE-2.0
	Unless required by applicable law or agreed to in writing, software
	distributed under the License is distributed on an "AS IS" BASIS,
	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
	See the License for the specific language governing permissions and
	limitations under the License.

*/
/*
 *+------------------------------------------------------------------------+
 
 
 *|                                                                        |
 *+------------------------------------------------------------------------+
*/

package com.ibm.dm.frontService.b.utils;

import java.util.Random;

public class RandomStrings {

	static final Random gen = new Random();

	public static String alphanumeric(int length) {
		StringBuilder b = new StringBuilder();

		for (int i = 0; i < length; ++i) {
			double x = gen.nextDouble();
			if (x < .1)
				b.append(gen.nextInt(10));
			else if (x < .55)
				b.append('a' + gen.nextInt('z' - 'a' + 1));
			else
				b.append('A' + gen.nextInt('Z' - 'A' + 1));
		}

		return b.toString();
	}

	public static String alphabetic(int length) {
		StringBuilder b = new StringBuilder();

		for (int i = 0; i < length; ++i) {
			double x = gen.nextDouble();
			if (x < .5)
				b.append('a' + gen.nextInt('z' - 'a' + 1));
			else
				b.append('A' + gen.nextInt('Z' - 'A' + 1));
		}

		return b.toString();
	}
}