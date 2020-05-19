import sys
import numpy as np

def getDataFile(file, num_agents):
        with open(file) as f:
                data = [line.replace('\"', '').split() for line in f]
        n = len(data)
        m = int(n/(num_agents+2))
        zi = np.empty(m)
        bm = np.empty(m)
        benchmark = np.empty(m)
        ts = np.empty(m)
        h_zi = np.empty(m)
        h_bm = np.empty(m)
        ah_zi = np.empty(m)
        ah_bm = np.empty(m)
        for i in range(m):
                ziC = 0
                bmC = 0
                ziH = 0
                ziAH = 0
                bmH = 0
                bmAH = 0
                for j in range(num_agents):
                        print(data[i * (num_agents+2) + j])
                        if data[i * (num_agents+2) + j][0] == 'zi':
                                ziC += float(data[i * (num_agents+2) + j][1])
                                #ziH += float(data[i * (num_agents+2) + j][2])
                                #ziAH += float(data[i * (num_agents+2) + j][3])
                        else:
                                bmC += float (data[i * (num_agents+2) + j][1])
                                #bmH += float(data[i * (num_agents+2) + j][2])
                                #bmAH += float(data[i * (num_agents+2) + j][3])
                benchmark[i] = float(data[i * (num_agents+2) + (num_agents)][0])
                ts[i] = float(data[i * (num_agents+2) + num_agents + 1][0])
                zi[i] = ziC
                bm[i] = bmC
                #h_zi[i] = ziH
                #ah_zi[i] = ziAH
                #h_bm[i] = bmH
                #ah_bm[i] = bmAH
        bmM = bm - benchmark * 40
        tsM = ts - benchmark * 40
        bmB = benchmark * 40
        return zi, bm, bmM, ts, tsM, h_zi, ah_zi, h_bm, ah_bm, bmB

def getDataList(data, num_agents):
        n = len(data)
        m = int(n/(num_agents+2))
        zi = np.empty(m)
        bm = np.empty(m)
        benchmark = np.empty(m)
        ts = np.empty(m)
        h_zi = np.empty(m)
        h_bm = np.empty(m)
        ah_zi = np.empty(m)
        ah_bm = np.empty(m)
        for i in range(m):
                ziC = 0
                bmC = 0
                ziH = 0
                ziAH = 0
                bmH = 0
                bmAH = 0
                for j in range(num_agents):
                        #print(data[i * (num_agents+2) + j])
                        if data[i * (num_agents+2) + j][0] == 'zi':
                                ziC += float(data[i * (num_agents+2) + j][1])
                                #ziH += float(data[i * (num_agents+2) + j][2])
                                #ziAH += float(data[i * (num_agents+2) + j][3])
                        else:
                                bmC += float (data[i * (num_agents+2) + j][1])
                                #bmH += float(data[i * (num_agents+2) + j][2])
                                #bmAH += float(data[i * (num_agents+2) + j][3])
                benchmark[i] = float(data[i * (num_agents+2) + (num_agents)][0])
                ts[i] = float(data[i * (num_agents+2) + num_agents + 1][0])
                zi[i] = ziC
                bm[i] = bmC
                #h_zi[i] = ziH
                #ah_zi[i] = ziAH
                #h_bm[i] = bmH
                #ah_bm[i] = bmAH
        bmM = bm - benchmark * 40
        tsM = ts - benchmark * 40
        bmB = benchmark * 40
        return zi, bm, bmM, ts, tsM, h_zi, ah_zi, h_bm, ah_bm, bmB

def getStats(agents):
        stats = '\tmean: ' + str(np.mean(agents)) + '\n'
        stats += '\ttotal: ' + str(np.sum(agents)) + '\n'
        stats += '\tsd: ' + str(np.std(agents)) + '\n'
        return stats

def getStatsJson(agents):
        stats = {}
        stats['mean']=np.mean(agents)
        stats['total']=np.sum(agents)
        stats['sd']=np.std(agents)
        return stats


def main():
        file = sys.argv[1]
        num_agents = int(sys.argv[2])
        zi, bm, bmM, ts, tsM, h_zi, ah_zi, h_bm, ah_bm, bmB = getDataFile(file, num_agents)
        print('ZI')
        print(getStats(zi))
        print('Benchmark Manipulator')
        print(getStats(bm))
        print('Benchmark Manipulator Market Performance')
        print(getStats(bmM))
        print('Benchmark Manipulator Benchmark Performance')
        print(getStats(bmB))
        print('Total Surplus')
        print(getStats(ts))
        print('Market Surplus')
        print(getStats(tsM))
        print('ZI Holdings')
        print(getStats(h_zi))
        print('ZI Abs Holdings')
        print(getStats(ah_zi))
        print('Benchmark Holdings')
        print(getStats(h_bm))
        print('Benchmark Abs Holdings')
        print(getStats(ah_bm))
        


if __name__ == '__main__':
        main()
