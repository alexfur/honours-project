package za.redbridge.experiment.HyperNEATM;

import org.encog.util.arrayutil.NormalizationAction;
import za.redbridge.experiment.NEATM.sensor.SensorModel;
import za.redbridge.experiment.NEATM.sensor.SensorType;
import za.redbridge.experiment.NEATM.sensor.parameter.spec.ParameterType;
import za.redbridge.experiment.NEATM.sensor.parameter.spec.Range;
import za.redbridge.experiment.Utils;
import org.encog.util.arrayutil.NormalizedField;

import static za.redbridge.experiment.NEATM.sensor.parameter.spec.ParameterType.BEARING;
import static za.redbridge.experiment.NEATM.sensor.parameter.spec.ParameterType.FIELD_OF_VIEW;
import static za.redbridge.experiment.NEATM.sensor.parameter.spec.ParameterType.ORIENTATION;
import static za.redbridge.experiment.NEATM.sensor.parameter.spec.ParameterType.RANGE;

import java.util.ArrayList;
import java.util.List;

/**
 * A builder that stores computes the sensor values for each input node of the substate.
 * Allows the transformation from values to sensor
 * Stores an arrayList for each sensor param
 * @TODO look into location translation to position
 */


public class HyperNEATMSensorBuilder {

    /**
     * The ID of this input node.
     */
    private int id;

    /**
     * An arrayList of all connection ranges.
     */
    private List<Float> ranges;

    /**
     * An arrayList of valid weights ie: above minimim.
     */
    private List<Float> weights;
    /**
     * An arrayList of weights.
     */
    private List<Float> FOVs;
    /**
     * An arrayList of orientation.
     */
    private List<Float> orientations;
    /**
     * An arrayList of orientation.
     */
    private List<Float> sensorTypes;

    private double[] SensorLocation;

    /**
     * Construct the builder with id.
     * @param id
     */


    public HyperNEATMSensorBuilder(int id, double[] location) {
        this.id =id;
        ranges = new ArrayList<>();
        weights = new ArrayList<>();
        FOVs= new ArrayList<>();
        orientations = new ArrayList<>();
        sensorTypes = new ArrayList<>();
        SensorLocation = location;

    }

    public void addRanges(Float range) {
        ranges.add(range);
    }

    public void addWeights(Float weight) {
        weights.add(weight);
    }

    public void addFOVs(Float FOV) {
        FOVs.add(FOV);
    }

    public void addOrientations(Float orientation) {
        orientations.add(orientation);
    }

    public void addSensorTypes(Float sensorType) {sensorTypes.add(sensorType);
    }
    /**
     * Checks if sensor should be created (ie: has valid connections
     */
    public boolean isValidSensor(){
        return (weights.size()>=1);
    }
    /**
     * creates a sensor model from param arrayLists
     */
    public SensorModel createSensorModel(){

        //Get type of sensor from the connection with max weight
        //@TODO look into if multiple maximums
        Float sensorTypeValue = sensorTypes.get(0);
        Float maxWeight = weights.get(0);
        for(int i =1;i<weights.size();i++){
            if (weights.get(i)>maxWeight){
                maxWeight = weights.get(i);
                sensorTypeValue = sensorTypes.get(i);
            }
        }
        SensorType sensorType =getSensorType(sensorTypeValue);
        return new SensorModel(sensorType, convertCartesianToRadians(),
                calculateAverage(orientations,sensorType,ParameterType.ORIENTATION),
                calculateAverage(ranges,sensorType,ParameterType.RANGE),
                calculateAverage(FOVs,sensorType,ParameterType.FIELD_OF_VIEW)
        );
   }
    //maps a float to a sensorType
    //@TODO implement actual map (for now just return
   public SensorType getSensorType(Float type){
        if (type <0){
            return SensorType.PROXIMITY;
        }
        else{
            return SensorType.ULTRASONIC;
        }
   }
   
   public float calculateAverage(List<Float> arrayList, SensorType type, ParameterType param){
        float sum = 0;
        for(int i =0;i<arrayList.size();i++){
            sum+=arrayList.get(i);
        }
        return normalise_range(sum/arrayList.size(),type, param);
   }

   public float convertCartesianToRadians(){
       return (float) za.redbridge.simulator.Utils.wrapAngle( Math.atan( SensorLocation[1]/SensorLocation[0]));

   }
   //CPPN gives range between [-1,1] so need to convert to appropriate sensor range
   public float normalise_range(float num, SensorType sensorType, ParameterType paramType) {
       Range range = sensorType.getDefaultSpecSet().getParameterSpec(paramType).getRange();
       NormalizedField normlalizer = new NormalizedField(NormalizationAction.SingleField, "", range.max, range.min, 1, -1);
       float value = (float) normlalizer.deNormalize(num);
       if (!range.inclusiveMin) {
           if (value <= range.min) {
               return Math.nextAfter(range.min, Double.POSITIVE_INFINITY);
           }
       }

       if (!range.inclusiveMax) {
           if (value >= range.max) {
               return Math.nextAfter(range.max, Double.NEGATIVE_INFINITY);
           }
       }

       return value;
   }
}