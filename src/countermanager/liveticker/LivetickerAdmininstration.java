/* Copyright (C) 2020 Christoph Theis */

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package countermanager.liveticker;

import countermanager.model.CounterModel;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author chtheis
 */
public class LivetickerAdmininstration {
    private LivetickerAdmininstration() {
        
    }
    
    // Liste der disabled Liveticker
    private static Set<Integer> disabled = new java.util.HashSet<>();
    
    @XmlRootElement
    private static class LivetickerList {
        @XmlElements({
            @XmlElement(name="ttm", type=countermanager.liveticker.ttm.TTM.class),
            @XmlElement(name="unas", type=countermanager.liveticker.unas.Unas.class)
        })
        @XmlElementWrapper(name="livetickers")
        List<countermanager.liveticker.Liveticker> list = new java.util.ArrayList<>();
    }
    
    private static LivetickerList liveticker = new LivetickerList();
    
    public static List<countermanager.liveticker.Liveticker> getLiveticker() {
        return liveticker.list;
    }
    
    
    public static void addLiveticker(countermanager.liveticker.Liveticker lt) {
        liveticker.list.add(lt);
    }
    
    public static void removeLiveticker(int idx) {
        liveticker.list.remove(idx);
    }
    
    public static countermanager.liveticker.Liveticker getLiveticker(int idx) {
        if (idx < 0 || idx >= liveticker.list.size())
            return null;
        
        return liveticker.list.get(idx);
    }
    
    public static void setLivetickerEnabled(int idx, boolean b) {
        countermanager.liveticker.Liveticker lt = getLiveticker(idx);
        if (lt == null)
            return;
        
        CounterModel.getDefaultInstance().removeCounterModelListener(lt);
        if (b && Liveticker.globalEnabled)
            CounterModel.getDefaultInstance().addCounterModelListener(lt);

        lt.setInstanceEnabled(b);
    }
    
    public static boolean isLivetickerEnabled(int idx) {
        Liveticker lt = getLiveticker(idx);
        if (lt == null)
            return false;
        
        return lt.isInstanceEnabled();
    }

    public static void setLivetickerEnabled(boolean b) {
        for (Liveticker lt : liveticker.list ) {
            CounterModel.getDefaultInstance().removeCounterModelListener(lt);
            if (b && lt.isInstanceEnabled())
                CounterModel.getDefaultInstance().addCounterModelListener(lt);
        }
        
        Liveticker.globalEnabled = b;
    }
    
    public static boolean isLivetickerEnabled() {
        return Liveticker.globalEnabled;
    }

    public static boolean isLivetickerDisabled(int counter) {
        return disabled.contains(counter);
    }

    public static void setLivetickerDisabled(int counter, boolean b) {
        if (b)
            disabled.add((counter));
        else
            disabled.remove(counter);
    }  
    
    
    private final static String ltFileName = "liveticker.xml";

    public static void storeLiveticker() {
        try {
            java.io.File dir = countermanager.prefs.Properties.getIniFile().getParentFile();
            JAXBContext context = JAXBContext.newInstance(LivetickerList.class);
            Marshaller m = context.createMarshaller();
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            m.marshal(liveticker, new java.io.FileWriter(new java.io.File(dir, ltFileName)));    
        } catch (JAXBException ex) {
            Logger.getLogger(LivetickerAdmininstration.class.getName()).log(Level.SEVERE, null, ex);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(LivetickerAdmininstration.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(LivetickerAdmininstration.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }
    
    public static void loadLiveticker() {
        for (countermanager.liveticker.Liveticker lt : liveticker.list) {
            CounterModel.getDefaultInstance().removeCounterModelListener(lt);
        }
        
        try {
            java.io.File dir = countermanager.prefs.Properties.getIniFile().getParentFile();
            JAXBContext context = JAXBContext.newInstance(LivetickerList.class);
            Unmarshaller um = context.createUnmarshaller();
            java.io.File ltFile = new java.io.File(dir, ltFileName);
            if (ltFile.exists())
                liveticker = (LivetickerList) um.unmarshal(new java.io.FileReader(ltFile));
            if (liveticker.list.isEmpty()) {
                liveticker.list.add(new countermanager.liveticker.ttm.TTM());
            }
        } catch (JAXBException ex) {
            Logger.getLogger(LivetickerAdmininstration.class.getName()).log(Level.SEVERE, null, ex);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(LivetickerAdmininstration.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }
}
