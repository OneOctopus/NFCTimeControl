/*
 * Copyright (c) 2016. OneOctopus www.oneoctopus.es
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package es.oneoctopus.nfctimecontrol.other;


import org.ndeftools.Record;

import java.util.ArrayList;
import java.util.List;

public class IterableMessage extends org.ndeftools.Message{

    public List<Record> getAllRecords (){
        List<Record> records = new ArrayList<>();
        for (int i=0; i<this.size(); i++)
            records.add(this.get(i));
        return records;
    }

}
