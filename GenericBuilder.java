import java.lang.reflect.*;
import java.math.BigDecimal;
import java.util.*;

/**
 * Created by guilherme.santos on 06/09/2017.
 * @param <T>
 */
public class GenericBuilder<T> {

    private Class<T> typeClass;

    private final int level;

    private static final String DEFAULT_IDENTIFIER = "_DEFAULT";
    private static final int DEFAULT_LEVEL_LIMIT = 1;
    private int LEVEL_LIMIT = DEFAULT_LEVEL_LIMIT;

    private static String GLOBAL_STRING_VALUE = "text";

    private boolean BOOLEAN_DEFAULT = true;
    private int INT_DEFAULT = 1;
    private long LONG_DEFAULT = 1L;
    private float FLOAT_DEFAULT = 1f;
    private double DOUBLE_DEFAULT = 1d;
    private BigDecimal BIG_DECIMAL_DEFAULT = new BigDecimal(1);
    private String STRING_DEFAULT = GLOBAL_STRING_VALUE;
    private char CHAR_DEFAULT = 'c';
    private byte BYTE_DEFAULT = 1;

    private boolean IGNORE_OBJECTS = true;
    private boolean BUILD_LISTS = false;

    private int DEFAULT_LIST_SIZE = 1;

    public GenericBuilder(Class<T> typeClass) {
        this.typeClass = typeClass;
        this.level = 0;
    }

    private GenericBuilder(Class<T> typeClass, int level, int levelLimit){
        this.typeClass = typeClass;
        this.level = ++level;
        this.LEVEL_LIMIT = levelLimit;
    }

    public GenericBuilder<T> setLevelLimit(int limit){
        LEVEL_LIMIT = limit >= 0 ? limit : 0;
        return this;
    }

    public GenericBuilder<T> setBooleanDefault(boolean booleanDefault){
        BOOLEAN_DEFAULT = booleanDefault;
        return this;
    }

    public boolean booleanDefault(){
        return BOOLEAN_DEFAULT;
    }

    public GenericBuilder<T> setIntDefault(int intDefault){
        INT_DEFAULT = intDefault;
        return this;
    }

    public int intDefault(){
        return INT_DEFAULT;
    }

    public GenericBuilder<T> setLongDefault(long longDefault){
        LONG_DEFAULT = longDefault;
        return this;
    }

    public long longDefault(){
        return LONG_DEFAULT;
    }

    public GenericBuilder<T> setFloatDefault(float floatDefault){
        FLOAT_DEFAULT = floatDefault;
        return this;
    }

    public float floatDefault(){
        return FLOAT_DEFAULT;
    }

    public GenericBuilder<T> setDoubleDefault(double doubleDefault){
        DOUBLE_DEFAULT = doubleDefault;
        return this;
    }

    public double doubleDefault(){
        return DOUBLE_DEFAULT;
    }

    public GenericBuilder<T> setBigDecimalDefault(BigDecimal bigDecimalDefault){
        BIG_DECIMAL_DEFAULT = bigDecimalDefault;
        return this;
    }

    public BigDecimal bigDecimalDefault(){
        return BIG_DECIMAL_DEFAULT;
    }

    public GenericBuilder<T> setCharDefault(char charDefault){
        CHAR_DEFAULT = charDefault;
        return this;
    }

    public char charDefault(){
        return CHAR_DEFAULT;
    }

    public GenericBuilder<T> setStringDefault(String stringDefault){
        STRING_DEFAULT = stringDefault;
        return this;
    }

    public String stringDefault(){
        return STRING_DEFAULT;
    }

    public GenericBuilder<T> setByteDefault(byte byteDefault){
        BYTE_DEFAULT = byteDefault;
        return this;
    }

    public byte byteDefault(){
        return BYTE_DEFAULT;
    }

    public GenericBuilder<T> doNotIgnoreObjects(){
        IGNORE_OBJECTS = false;
        return this;
    }

    public GenericBuilder<T> buildInnerLists() {
        BUILD_LISTS = true;
        return this;
    }

    public static void setGlobalStringValue(String value){
        GLOBAL_STRING_VALUE = value;
    }


    public GenericBuilder<T> setDefaultListSize(int defaultListSize){
        DEFAULT_LIST_SIZE = defaultListSize > 0 ? defaultListSize : DEFAULT_LIST_SIZE;
        return this;
    }

    public int defaultListSize(){
        return DEFAULT_LIST_SIZE;
    }

    private <Z> Z quickBuild(Class<Z> typeClass){
        return new GenericBuilder<>(typeClass, level, LEVEL_LIMIT).setAllDefaults(getAllDefaults()).setDefaultListSize(defaultListSize()).getObject().build();
    }

    private GenericBuilder<T> setAllDefaults(Map<String, Object> defaults){
        for(Field field : this.getClass().getDeclaredFields()){
            if(field.getName().endsWith(DEFAULT_IDENTIFIER)){
                try {
                    field.set(this, defaults.get(field.getName()));
                } catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
        return this;
    }

    private Map<String, Object> getAllDefaults(){
        Map<String, Object> defaults = new LinkedHashMap<>();
        for (Field field : this.getClass().getDeclaredFields()){
            if(field.getName().endsWith(DEFAULT_IDENTIFIER)){
                try {
                    defaults.put(field.getName(), field.get(this));
                } catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
        return defaults;
    }

    public boolean ignoreObjects(){
        return IGNORE_OBJECTS;
    }

    public ObjectBuilder getObject(){
        return new ObjectBuilder();
    }

    public class ObjectBuilder {

        private T instance;

        @SuppressWarnings("unchecked")
        private ObjectBuilder(){
            this.instance = (T) getObjectInstance(typeClass);
        }

        private Object getObjectInstance(Class<?> typeClass) {
            return checkForType(typeClass);
        }

        private <Z> Z populateInstance(Z instance){

            Class typeClass = instance.getClass();
            List<Method> setMethods = new ArrayList<>();
            List<List<Parameter>> methodParams = new ArrayList<>();

            setMethods(typeClass.getDeclaredFields(), typeClass.getDeclaredMethods(), setMethods, methodParams);

            Class superClass = typeClass.getSuperclass();
            while(superClass != null){
                setMethods(superClass.getDeclaredFields(), superClass.getDeclaredMethods(), setMethods, methodParams);
                superClass = superClass.getSuperclass();
            }
            try {
                for (int i = 0; i < setMethods.size(); i++) {
                    setMethods.get(i).invoke(instance, getPopulatedParams(methodParams.get(i)));
                }
            } catch (Exception e){
                e.printStackTrace();
            }
            return instance;

        }

        private void setMethods(Field[] fieldList, Method[] methodList, List<Method> setMethods, List<List<Parameter>> methodParams){
            for(Field field : fieldList){
                for (Method method : methodList){
                    if (method.getName().toLowerCase().contains(field.getName().toLowerCase()) && method.getName().toLowerCase().contains("set")){
                        setMethods.add(method);
                        methodParams.add(new ArrayList<>(Arrays.asList(method.getParameters())));
                    }
                }
            }
        }

        private Object[] getPopulatedParams(List<Parameter> parameters){
            List<Object> populatedParams = new ArrayList<>();
            if (level < LEVEL_LIMIT) {
                for (Parameter parameter : parameters) {
                    populatedParams.add(populateParam(parameter));
                }
            } else {
                for (Parameter parameter : parameters) {
                    populatedParams.add(getObjectInstance(parameter.getType()));
                }
            }
            return populatedParams.toArray();
        }

        private Object populateParam(Parameter parameter){
                if((parameter.getType().equals(List.class)) && BUILD_LISTS){
                    String typeName = parameter.getAnnotatedType().getType().getTypeName();
                    String typeParamName = typeName.split("<")[1].substring(0, typeName.split("<")[1].length()-1);
                    try {
                            return new GenericBuilder<>(Class.forName(typeParamName), level, LEVEL_LIMIT)
                                .setDefaultListSize(defaultListSize())
                                .setAllDefaults(getAllDefaults())
                                .getObject()
                                .buildList();
                    } catch (Exception e){
                        e.printStackTrace();
                        return quickBuild(parameter.getType());
                    }
                } else {
                    return quickBuild(parameter.getType());
                }
        }

        private <Z> Object populateArray(Class<Z> type){
            Object array = Array.newInstance(type, DEFAULT_LIST_SIZE);
            for(int i = 0; i < Array.getLength(array); i++){
                Array.set(array, i, quickBuild(type));
            }
            return array;
        }

        private <Z> Object checkForType(Class<Z> type){
            if(ignoreObjects() && type.equals(Object.class)){
                return null;
            }
            if(type.isEnum()){
                return getEnumValue(type);
            }
            if(type.isArray()){
                return populateArray(type.getComponentType());
            }
            switch (type.getName()){
                case "java.util.List":
                    return new ArrayList<>();
                case "java.util.Map":
                    return new HashMap<>();
                case "java.math.BigDecimal":
                    return bigDecimalDefault();
                case "long":
                case "java.lang.Long":
                    return longDefault();
                case "boolean":
                case "java.lang.Boolean":
                    return booleanDefault();
                case "int":
                case "java.lang.Integer":
                    return intDefault();
                case "char":
                case "java.lang.Character":
                    return charDefault();
                case "float":
                case "java.lang.Float":
                    return floatDefault();
                case "double":
                case "java.lang.Double":
                    return doubleDefault();
                case "java.lang.String":
                    return stringDefault();
                case "byte":
                case "java.lang.Byte":
                    return byteDefault();
            }
            if(type.isInterface()){
                return getInterfaceInstance(type);
            }
            try {
                return type.getConstructor().newInstance();
            } catch (Exception e){
                return null;
            }
        }

        @SuppressWarnings("unchecked")
        private T getInterfaceInstance(Class<?> typeClass){
            InvocationHandler handler = (proxy, method, args) -> quickBuild(method.getReturnType());
            return (T) Proxy.newProxyInstance(typeClass.getClassLoader(), new Class<?>[]{typeClass}, handler);
        }

        private Object getEnumValue(Class<?> typeClass){
            Object[] constants = typeClass.getEnumConstants();
            return constants.length > 0 ? constants[0] : null;
        }

        private List<T> getList(int size){
            List<T> list = new ArrayList<>();
            instance = populateInstance(instance);
            for(int i = 0; i < size; i++){
                list.add(instance);
            }
            return list;
        }

        public T build() {
            return instance == null ? null : populateInstance(instance);
        }

        public List<T> buildList(){
            return getList(defaultListSize());
        }

        public List<T> buildList(int size){
            return getList(size);
        }

        public T buildEmpty(){
            return instance;
        }


    }

}

