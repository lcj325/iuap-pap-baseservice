package com.yonyou.iuap.baseservice.intg.service;

import com.yonyou.iuap.baseservice.entity.Model;
import com.yonyou.iuap.baseservice.intg.support.ServiceFeature;
import com.yonyou.iuap.baseservice.intg.support.ServiceFeatureHolder;
import com.yonyou.iuap.baseservice.persistence.mybatis.mapper.GenericMapper;
import com.yonyou.iuap.baseservice.persistence.support.DeleteFeatureExtension;
import com.yonyou.iuap.baseservice.persistence.support.QueryFeatureExtension;
import com.yonyou.iuap.baseservice.persistence.support.SaveFeatureExtension;
import com.yonyou.iuap.baseservice.service.GenericService;
import com.yonyou.iuap.mvc.type.SearchParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

import static com.yonyou.iuap.baseservice.intg.support.ServiceFeature.LOGICAL_DEL;

/**
 * 特性集成服务,用于GenericService所有服务接口集成多种组件特性
 * 默认支持
 *      <p>REFERENCE-参照
 * @param <T>
 */
@SuppressWarnings("ALL")
public  abstract class GenericIntegrateService<T extends Model> extends GenericService<T> {
    private static Logger log = LoggerFactory.getLogger(GenericIntegrateService.class);
    private static final String LOG_TEMPLATE="特性组件{}的未实现{}扩展" ;


    /**
     * 在执行查询方法前，仅能通过泛型来拿到实体的真实类型
     * @return 泛型指定的业务实体Class
     */
    private Class  getModelClass(){
        Type superclassType = this.getClass().getGenericSuperclass();
        if (!ParameterizedType.class.isAssignableFrom(superclassType.getClass())) {
            log.warn("泛型解析失败，可能影响特性扩展服务");
            return Model.class ;
        }
        Type[] t = ((ParameterizedType) superclassType).getActualTypeArguments();
        if (null!=t&& t.length>0){
            return (Class) t[0];
        }else
        {
            return Model.class ;
        }
    }


    protected Set<QueryFeatureExtension> customQueryExts;     //客户自定义的查询特性服务扩展点
    protected Set<SaveFeatureExtension> customSaveExts;       //客户自定义的保存特性服务扩展点
    protected Set<DeleteFeatureExtension> customDeleteExts;   //客户自定义的删除特性服务扩展点

    /***************************************************/
    /**
     * 查询前置条件处理埋点
     * @param searchParams
     * @return
     */
    private SearchParams  prepareFeatSearchParam(SearchParams searchParams){

        for (String feat:feats){
           QueryFeatureExtension instance = ServiceFeatureHolder.getQueryExtension(feat);
           if (instance==null){
                log.info(LOG_TEMPLATE, feat,QueryFeatureExtension.class.getSimpleName());
           }else{
               searchParams= instance.prepareQueryParam(searchParams,getModelClass());
           }
        }
        if ( customQueryExts == null){
            customQueryExts = ServiceFeatureHolder.getModelExtensions(getModelClass(), QueryFeatureExtension.class);
        }
        for (QueryFeatureExtension qService : customQueryExts){
            searchParams= qService.prepareQueryParam(searchParams,getModelClass());
        }
        return searchParams;
    }

    /**
     * 查询后续处理埋点
     * @param list
     * @return
     */
    private List fillListFeatAfterQuery(List list){
        for (String feat:feats){
            QueryFeatureExtension instance = ServiceFeatureHolder.getQueryExtension(feat);
            if (instance==null){
                log.info(LOG_TEMPLATE, feat,QueryFeatureExtension.class);
            }else{
                list= instance.afterListQuery(list);
            }
        }
        if ( customQueryExts == null){
            customQueryExts = ServiceFeatureHolder.getModelExtensions(getModelClass(), QueryFeatureExtension.class);
        }
        for (QueryFeatureExtension qService : customQueryExts){
            list= qService.afterListQuery(list);
        }
        return list;
    }


    /**
     * 分页查询
     * @param pageRequest
     * @param searchParams
     * @return
     */
    @Override
    public Page<T> selectAllByPage(PageRequest pageRequest, SearchParams searchParams) {

        searchParams=prepareFeatSearchParam(searchParams);
        Page<T> page=super.selectAllByPage(pageRequest, searchParams);
        fillListFeatAfterQuery(page.getContent());
        return page;
    }

    /**
     * 查询所有数据
     * @return
     */
    @Override
    public List<T> findAll(){
        SearchParams searchParams=prepareFeatSearchParam(new SearchParams());
        return   fillListFeatAfterQuery(super.queryList(searchParams.getSearchMap()));
    }

    /**
     * 根据参数查询List
     * @param queryParams
     * @return
     */
    @Override
    public List<T> queryList(Map<String,Object> queryParams){
        SearchParams searchParams= new SearchParams();
        searchParams.setSearchMap(queryParams);
        searchParams=prepareFeatSearchParam(searchParams);
        return   fillListFeatAfterQuery(super.queryList(searchParams.getSearchMap()));
    }

    /**
     * 根据字段名查询List
     * @param name
     * @param value
     * @return
     */
    @Override
    public List<T> queryList(String name, Object value){
        SearchParams searchParams= new SearchParams();
        searchParams.addCondition(name,value);
        searchParams=prepareFeatSearchParam(searchParams);
        return fillListFeatAfterQuery(super.queryList(searchParams.getSearchMap()));
    }

    /**
     * 根据参数查询List【返回值为List<Map>】
     * @param params 查询条件参数
     * @return 查询动态结果集
     */
    @Override
    public List<Map<String,Object>> queryListByMap(Map<String,Object> params){

//        return super.queryListByMap(params);
        SearchParams searchParams= new SearchParams();
        searchParams.setSearchMap(params);
        searchParams=prepareFeatSearchParam(searchParams);
        List<Map<String,Object> >list=super.queryListByMap(searchParams.getSearchMap());
        return   fillListFeatAfterQuery(list);
    }

    /**
     * 根据ID查询数据
     * @param id 主键
     * @return 带特性加工的业务实体
     */
    @Override
    public T findById(Serializable id) {
       return  this.findUnique("id",id);
    }

    /**
     * 查询唯一数据
     * @param name 查询参数名
     * @param value 查询匹配值
     * @return 带特性加工的业务实体
     */
    @Override
    public T findUnique(String name, Object value) {
        SearchParams searchParams= new SearchParams();
        searchParams.addCondition(name,value);
        searchParams=prepareFeatSearchParam(searchParams);
        List<T>listData=super.queryList(searchParams.getSearchMap());
        listData=fillListFeatAfterQuery(listData);
        if(listData!=null && listData.size()==1) {
            return listData.get(0);
        }else {
            throw new RuntimeException("检索数据不唯一, "+name + ":" + value);
        }
    }

    /***************************************************/
    /**
     * 保存前按特性初始化entity
     * @param entity 保存前的业务实体
     */
    private void prepareFeatEntity(T entity){
        for (String feat:feats){
            SaveFeatureExtension instance = ServiceFeatureHolder.getSaveExtension(feat);
            if (instance==null){
                log.info(LOG_TEMPLATE, feat,SaveFeatureExtension.class);
            }else{
                instance.prepareEntityBeforeSave(entity);
            }
        }
        if ( customSaveExts == null){
            customSaveExts = ServiceFeatureHolder.getModelExtensions(getModelClass(), SaveFeatureExtension.class);
        }
        for (SaveFeatureExtension sService : customSaveExts){
            sService.prepareEntityBeforeSave(entity);
        }
    }

    /**
     * 保存实体之后的特性扩展埋点
     * @param entity
     */
    private void addFeatAfterEntitySave(T entity){
        for (String feat:feats){
            SaveFeatureExtension instance = ServiceFeatureHolder.getSaveExtension(feat);
            if (instance==null){
                log.info(LOG_TEMPLATE, feat,SaveFeatureExtension.class);
            }else{
                instance.afterEntitySave(entity);
            }
        }
        if ( customSaveExts == null){
            customSaveExts = ServiceFeatureHolder.getModelExtensions(getModelClass(), SaveFeatureExtension.class);
        }
        for (SaveFeatureExtension sService : customSaveExts){
            sService.afterEntitySave(entity);
        }
    }

    /**
     * 保存数据,不需埋点,因为其后续会调用insert 或 update的埋点
     * @param entity
     * @return
     */
    @Override
    public T save(T entity) {
//        prepareFeatEntity(entity);
        entity=super.save(entity);
//        addFeatAfterEntitySave(entity);
        return entity;
    }

    /**
     * 批量保存
     * @param listEntity
     */
    @Override
    public void saveBatch(List<T> listEntity){
        for(int i=0; i<listEntity.size(); i++) {
            this.save(listEntity.get(i));
        }
    }

    /**
     * 新增保存数据
     * @param entity
     * @return
     */
    @Override
    public T insert(T entity) {
        prepareFeatEntity(entity);
        try {
            entity=super.insert(entity);
        } catch (Exception e) {
            if (e instanceof  DuplicateKeyException){
                throw new RuntimeException("违反唯一性约束，无法保存");
            }else{
                throw e;
            }
        }
        addFeatAfterEntitySave(entity);
        return entity;
    }

    /**
     * 更新保存数据
     * @param entity
     * @return
     */
    @Override
    public T update(T entity) {
        prepareFeatEntity(entity);
        try {
            entity=super.update(entity);
        } catch (Exception e) {
            if (e instanceof DuplicateKeyException){
                throw new RuntimeException("违反唯一性约束，无法保存");
            }else{
                throw e;
            }
        }
        addFeatAfterEntitySave(entity);
        return entity;
    }
    /***************************************************/
    /**
     * 删除前操作业务实体entity,或查询条件 params
     * @param entity 单实体删除时传参
     * @param params 批量删除时的查询条件
     */
    private void prepareFeatDeleteParam(T entity,Map params){
        for (String feat:feats){
            DeleteFeatureExtension instance = ServiceFeatureHolder.getDeleteExtension(feat);
            if (instance==null){
                log.info(LOG_TEMPLATE, feat,DeleteFeatureExtension.class);
            }else{
                instance.prepareDeleteParams(entity,params);
            }
        }
        if ( customDeleteExts == null){
            customDeleteExts = ServiceFeatureHolder.getModelExtensions(getModelClass(), DeleteFeatureExtension.class);
        }
        for (DeleteFeatureExtension dService : customDeleteExts){
            dService.prepareDeleteParams(entity,params);
        }
    }

    /**
     * 删除后特性扩展埋点
     * @param entity 被删除的业务实体
     */
    private void runFeatAfterEntityDelete(T entity) {
        for (String feat : feats) {
            DeleteFeatureExtension instance = ServiceFeatureHolder.getDeleteExtension(feat);
            if (instance == null) {
                log.info(LOG_TEMPLATE, feat,DeleteFeatureExtension.class);
            } else {
                instance.afterDeteleEntity(entity);
            }
        }
        if ( customDeleteExts == null){
            customDeleteExts = ServiceFeatureHolder.getModelExtensions(getModelClass(), DeleteFeatureExtension.class);
        }
        for (DeleteFeatureExtension dService : customDeleteExts){
            dService.afterDeteleEntity(entity);
        }
    }
            /**
             * 删除数据
             */
    @Override
    public int deleteBatch(List<T> list) {
        int count = 0;
        for(T entity: list) {
            count += this.delete(entity.getId());
        }
        return count;
    }

    /**
     * 删除数据:核心集成点
     * @param entity
     * @return
     */
    @Override
    public int delete(T entity) {
        Map params = new  HashMap <>();
        params.put("id",entity.getId());
        prepareFeatDeleteParam(entity,params);
        int count= this.intgDelete(entity,params);
        runFeatAfterEntityDelete(entity);
        return count;
    }

    /**
     * 根据id删除数据
     * @param id
     * @return
     */
    @Override
    public int delete(Serializable id) {
        Map params = new  HashMap <>();
        params.put("id",id);
        List<T> ls = genericMapper.queryList(params);
        if (ls!=null&&ls.size()>0){
            return  this.delete(ls.get(0));
        }else{
            log.info("删除失败,无id为{}的数据",id);
//            throw new RuntimeException("删除失败,无id为{}的数据");
            return 0;
        }
    }

    /**
     * 物理删除:任意态
     * @param params
     * @return
     */
    public int intgDelete(T entity,Map params) {
        if (isLogicalDel()){
            super.update(entity);
            return 1;
        }else{
            return this.genericMapper.delete(params);
        }

    }

    private boolean isLogicalDel(){
        for (String feat:feats){
            if (LOGICAL_DEL.name().equalsIgnoreCase(feat )) return true;
        }
        return false;
    }

    /***************************************************/

    protected GenericMapper<T> genericMapper;

    protected String[] feats = new String[]{"REFERENCE"};

    protected abstract ServiceFeature[] getFeats();

    public void setGenericMapper(GenericMapper<T> mapper ) {
        setGenericMapper(mapper,new String[0]);
    }

    public void setGenericMapper(GenericMapper<T> mapper,String... extensions ) {
        this.feats=combineFeats(extensions);
        this.genericMapper = mapper;
        super.setGenericMapper(mapper);
    }

    private String[] combineFeats(String[] extensions){
        List<String> allFeat = new ArrayList<>();

        for (ServiceFeature feature:getFeats()){
            allFeat.add(feature.name());
        }
        for (String ex:extensions){
            allFeat.add(ex);
        }
        return  allFeat.toArray(extensions);
    }

}
