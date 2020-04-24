/* Copyright (C) 2020 Christoph Theis */
package countermanager.liveticker;

import javax.xml.bind.annotation.adapters.XmlAdapter;

public class XmlPasswordAdapter extends XmlAdapter<String, String> {

    @Override
    public String unmarshal(String pwd) throws Exception {
        return Liveticker.decryptPassword(pwd);
    }

    @Override
    public String marshal(String pwd) throws Exception {
        return Liveticker.encryptPassword(pwd);
    }

}
    
