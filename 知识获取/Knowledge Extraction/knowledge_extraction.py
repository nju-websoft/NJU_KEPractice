# coding=utf-8
import re
from pyltp import Segmentor, Postagger, Parser, NamedEntityRecognizer

# 加载pyltp模型
segmentor = Segmentor()
segmentor.load_with_lexicon("./pyltp_models/cws.model", './construct_dict.txt')  # 分词模型
postagger = Postagger()
postagger.load("./pyltp_models/pos.model")  # 词性标注
parser = Parser()
parser.load("./pyltp_models/parser.model")  # 依存句法分析
recognizer = NamedEntityRecognizer()
recognizer.load("./pyltp_models/ner.model")  # 命名实体识别

# 文件定义
input_file = "input.txt"  # 输入文本文件
temp_file = "tmp.txt"  # 中间处理文件
output_file = "output.txt"  # 输出文本文件
dict_file = "dict.txt"  # 字典文件
triple_file = "data.nt"  # 最终输出文件

construct_list = []


def get_dict_list():
    """
    获取词典内容
    """
    f = open(dict_file, 'r', encoding="utf-8")
    for line in f:
        construct = line.strip()
        if construct not in construct_list:
            construct_list.append(construct)


def map_wordlist_constructlist(word_list):
    """
    检测word_list是否存在于字典中
    """
    for word in construct_list:
        if word == word_list:
            return True
    return False


def doc2sent():
    """
    将input_file中的文本进行分句，并保存在temp_file中
    """
    with open(input_file, 'r', encoding='utf-8') as in_file, open(temp_file, 'w', encoding='utf-8') as tmp_file:
        inputs = in_file.read()
        sents = re.split('[。]', inputs)
        sents = '\n'.join(sents)
        tmp_file.writelines(sents)


def extraction_start():
    """
    事实三元组抽取的总控程序
    """
    for sentence_index, text_line in enumerate(in_file):
        sentence = text_line.strip()
        if len(sentence) == 0:
            continue
        fact_knowledge_extract(sentence)


def fact_knowledge_extract(sentence):
    """
    对于给定的句子进行事实三元组抽取
    """
    words = segmentor.segment(sentence)
    postags = postagger.postag(words)
    netags = recognizer.recognize(words, postags)
    arcs = parser.parse(words, postags)

    child_dict_list = build_parse_child_dict(words, postags, arcs)

    for index in range(len(postags)):
        # 抽取以谓词为中心的事实三元组
        if postags[index] == 'v':
            child_dict = child_dict_list[index]
            # 主谓宾
            if 'SBV' in child_dict and 'VOB' in child_dict:
                cur_wordlist = []
                e1 = complete_entity(words, postags, child_dict_list, child_dict['SBV'][0], cur_wordlist)
                r = words[index]
                e2 = complete_entity(words, postags, child_dict_list, child_dict['VOB'][0], cur_wordlist)
                if map_wordlist_constructlist(cur_wordlist):
                    out_file.write("({}, {}, {})\n".format(e1, r, e2))

                if 'COO' in child_dict:  # 寻找并列关系
                    tie_index = child_dict['COO'][0]
                    new_child_dict = child_dict_list[tie_index]
                    if 'VOB' in new_child_dict:
                        cur_wordlist = []
                        e1 = complete_entity(words, postags, child_dict_list, child_dict['SBV'][0], cur_wordlist)
                        r = words[tie_index]
                        e2 = complete_entity(words, postags, child_dict_list, new_child_dict['VOB'][0], cur_wordlist)
                        if map_wordlist_constructlist(cur_wordlist):
                            out_file.write("({}, {}, {})\n".format(e1, r, e2))

        # 尝试抽取命名实体有关的三元组
        if netags[index][0] == 'S' or netags[index][0] == 'B':
            ni = index
            if netags[ni][0] == 'B':
                for i in range(index, len(postags)):
                    if netags[i][0] == 'E':
                        ni = i
                        break
                e1 = ''.join(words[index:ni+1])
            else:
                e1 = words[ni]

            if arcs[ni].relation == 'ATT' and postags[arcs[ni].head-1] == 'n' and netags[arcs[ni].head-1] == 'O':
                cur_wordlist = []
                r = complete_entity(words, postags, child_dict_list, arcs[ni].head-1, cur_wordlist)
                if e1 in r:
                    r = r[(r.index(e1)+len(e1)):]
                if arcs[arcs[ni].head-1].relation == 'ATT' and netags[arcs[arcs[ni].head-1].head-1] != 'O':
                    e2 = complete_entity(words, postags, child_dict_list, arcs[arcs[ni].head-1].head-1, cur_wordlist)
                    mi = arcs[arcs[ni].head-1].head-1
                    li = mi
                    if netags[mi][0] == 'B':
                        while netags[mi][0] != 'E':
                            mi += 1
                        e = ''.join(words[li+1:mi+1])
                        e2 += e
                    if r in e2:
                        e2 = e2[(e2.index(r)+len(r)):]
                    out_file.write("(%s, %s, %s)\n" % (e1, r, e2))

    # 补充抽取一些实体相关三元组
    triple_extraction_supplement(words, postags, netags, arcs)


def triple_extraction_supplement(words, postags, netags, arcs):
    def complete_construction(words, child_dict_list, word_index, is_head):
        child_dict = child_dict_list[word_index]
        prefix = ''
        if 'ATT' in child_dict:
            if is_head:
                for i in child_dict['ATT']:
                    prefix += words[i]
            else:
                for i in child_dict['ATT'][1:]:
                    prefix += words[i]
        return prefix + words[word_index]

    child_dict_list = build_parse_child_dict(words, postags, arcs)
    for index in range(len(postags)):
        if netags[index][0] == 'S':
            pre_child_dict = child_dict_list[index - 1]
            if 'ATT' in pre_child_dict:
                first_entity_index = pre_child_dict['ATT'][0]
                if 'ATT' in child_dict_list[first_entity_index]:
                    e1_index = child_dict_list[first_entity_index]['ATT'][0]
                    e1 = complete_construction(words, child_dict_list, e1_index, True) + words[first_entity_index]
                    relation = complete_construction(words, child_dict_list, index - 1, False)
                    e2 = words[index]
                    out_file.write("(%s, %s, %s)\n" % (e1, relation, e2))

                if 'LAD' in pre_child_dict:  # 并列结构
                    for lad_entity_index in pre_child_dict['LAD']:
                        tie_entity_index = lad_entity_index - 1
                        if 'ATT' in child_dict_list[tie_entity_index]:
                            e1_index = child_dict_list[tie_entity_index]['ATT'][0]
                            e1 = complete_construction(words, child_dict_list, e1_index, True)
                            relation = complete_construction(words, child_dict_list, tie_entity_index, False)
                            e2 = words[index]
                            out_file.write("(%s, %s, %s)\n" % (e1, relation, e2))


def build_parse_child_dict(words, postags, arcs):
    """
    为句子中的每个词语维护一个保存句法依存儿子节点的字典
    Args:
        words: 分词列表
        postags: 词性列表
        arcs: 句法依存列表, head表示父节点索引，relation表示依存弧的关系
    """
    child_dict_list = []
    for index in range(len(words)):
        child_dict = {}
        for arc_index in range(len(arcs)):
            if arcs[arc_index].head == index + 1:
                relation = arcs[arc_index].relation
                if relation not in child_dict:
                    child_dict[relation] = []
                child_dict[relation].append(arc_index)
        child_dict_list.append(child_dict)
    return child_dict_list


def complete_entity(words, postags, child_dict_list, word_index, wordlist):
    """
    完善识别的部分实体
    """
    child_dict = child_dict_list[word_index]
    prefix = ''
    if 'ATT' in child_dict:
        for i in range(len(child_dict['ATT'])):
            prefix += complete_entity(words, postags, child_dict_list, child_dict['ATT'][i], wordlist)
    postfix = ''
    if postags[word_index] == 'v':
        if 'VOB' in child_dict:
            postfix += complete_entity(words, postags, child_dict_list, child_dict['VOB'][0], wordlist)
        if 'SBV' in child_dict:
            prefix = complete_entity(words, postags, child_dict_list, child_dict['SBV'][0], wordlist) + prefix
        if 'FOB' in child_dict:
            prefix += complete_entity(words, postags, child_dict_list, child_dict['FOB'][0], wordlist)

    tie_entity = ''
    if 'COO' in child_dict:
        for i in range(len(child_dict['COO'])):
            tie_entity += complete_entity(words, postags, child_dict_list, child_dict['COO'][i], wordlist)
        if len(tie_entity) > 0:
            tie_entity = '|' + tie_entity
    wordlist.append(words[word_index])
    return prefix + words[word_index] + postfix + tie_entity


def triple2nt():
    """
    将output.txt中的三元组转化成带有前缀的三元组并保存在data.nt中
    """
    prefix = ''
    with open('virtuoso_config.properties', 'r', encoding='utf-8') as f:
        for line in f.readlines():
            line = line.split(' = ')
            if line[0] == 'tcqaPrefix':
                prefix = line[1]
                start = prefix.find('<')
                prefix = prefix[start:-2]  # 包含<,不包含>
                break
    with open(output_file, 'r', encoding='utf-8') as f, open(triple_file, 'w', encoding='utf-8') as out:
        triples = []
        for line in f:
            s, p, o = line.rstrip('\n')[1:-1].split(', ')
            if '|' in s:
                continue
            if '|' in p:
                p1, p2 = p.split('|')
                triples.append((s, p1, o))
                triples.append((s, p2, o))
            else:
                triples.append((s, p, o))
        for s, p, o in triples:
            if map_wordlist_constructlist(o):
                out.write(
                    '%s%s>\t%s%s>\t%s%s>.\n' % (prefix, s, prefix, p, prefix, o))
            else:
                out.write(
                    '%s%s>\t%s%s>\t%s.\n' % (prefix, s, prefix, p, o))


if __name__ == "__main__":
    doc2sent()
    get_dict_list()
    with open(temp_file, 'r', encoding='utf-8') as in_file, open(output_file, 'w', encoding='utf-8') as out_file:
        extraction_start()
    triple2nt()
